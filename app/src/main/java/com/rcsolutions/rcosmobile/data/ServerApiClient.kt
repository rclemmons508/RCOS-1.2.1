package com.rcsolutions.rcosmobile.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.rcsolutions.rcosmobile.BuildConfig
import com.rcsolutions.rcosmobile.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "ServerApi"

/**
 * Phase 2 — Railway server client.
 *
 * Reuses the old app's API contracts exactly (RCOS-MOBILE-APP `server.ts`).
 * No endpoint, field name, or collection is invented here. Every call
 * returns an [ApiResult] — a failure is always surfaced to the caller,
 * never faked as success.
 *
 * Auth: the signed-in user's Firebase ID token is sent as
 * `Authorization: Bearer <token>` (same as the old app's `apiFetch` in
 * `src/lib/api.ts`). The server gate (`server/auth.ts`) answers 401 for a
 * missing/expired token and 503 while the gate is unconfigured. Calls
 * that must never accept a client-asserted identity (Gmail token,
 * Gmail OAuth start, quote/invoice creation) go through the server's
 * `requireVerifiedAuth`, which denies outright while the gate is off.
 *
 * Base URL: [BuildConfig.RCOS_API_BASE_URL]. Default is the live Railway
 * server; override with the `rcosApiBaseUrl` Gradle property
 * (`-PrcosApiBaseUrl=...` or `local.properties`). No API key, LiveKit
 * secret, Vonage key, or Firebase key lives in this file or in git.
 */

// ---- Responses (shapes copied from the old server, server.ts) ----

/** GET /api/health — public. `{"status":"ok","timestamp":"..."}` */
@Serializable
data class HealthResponse(
    val status: String = "",
    val timestamp: String = "",
)

/** GET /api/firebase-config — public. */
@Serializable
data class FirebaseConfigResponse(
    val telephonyProvider: String = "",
    val projectId: String = "",
    val firestoreDatabaseId: String = "",
    val storageBucket: String = "",
)

/** One row of GET /api/diagnostics/scan. */
@Serializable
data class DiagnosticCheck(
    val id: String = "",
    val label: String = "",
    val status: String = "",
    val detail: String = "",
)

/** GET /api/diagnostics/scan. Requires a signed-in user — the server
 * login gate applies to it (verified live: unsigned call answers 401),
 * even though the rate limiter exempts it. */
@Serializable
data class DiagnosticsScan(
    val overall: String = "",
    val score: Int = 0,
    val checks: List<DiagnosticCheck> = emptyList(),
    val scannedAt: String = "",
)

/** POST /api/chat request message. Role mapping mirrors the server:
 * `assistant`/`model` -> `model`, anything else -> `user`. */
@Serializable
data class ChatApiMessage(
    val role: String,
    val content: String,
)

/** POST /api/chat request. `roleId` defaults server-side to
 * `executive_assistant`; `modelTier` is `general` | `complex` | `fast`. */
@Serializable
data class ChatRequest(
    val messages: List<ChatApiMessage>,
    val roleId: String = "executive_assistant",
    val modelTier: String = "general",
    val businessId: String = "",
)

/** POST /api/chat response. When Gemini is down the server still
 * answers HTTP 200 with its own fallback text and
 * `modelUsed == "rcos-offline-engine"` — see [ChatReply.isFallback]. */
@Serializable
data class ChatReply(
    val role: String = "assistant",
    val content: String = "",
    val modelUsed: String = "",
    val roleId: String = "",
    val timestamp: String = "",
) {
    /** True when the server answered with its offline fallback instead
     * of a live Gemini reply. The text itself already says so; callers
     * must not present it as a live answer. */
    val isFallback: Boolean get() = modelUsed == "rcos-offline-engine"
}

/** GET /api/business/{id}/gmail-token — the caller's OWN Gmail access
 * token only (server proves identity from the verified Firebase token,
 * never from a client-sent email). 404 means Gmail is not connected
 * for this user yet. The token is held in memory only, never written
 * to disk or to Firestore by the app. */
@Serializable
data class GmailTokenResponse(
    val accessToken: String = "",
    val email: String = "",
)

/** POST /api/business/{id}/gmail/oauth-start — returns the Google
 * OAuth URL the app opens in the system browser. Tokens are stored
 * server-side in Firestore (`serverState` doc, AES-256-GCM envelope
 * when TOKEN_ENCRYPTION_KEY is set) — never in a server local file. */
@Serializable
data class GmailOAuthStart(
    val url: String = "",
    val state: String = "",
    val expiresAt: String = "",
)

// ---- Result type ----

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class HttpError(val status: Int, val message: String) : ApiResult<Nothing>
    data class NetworkError(val message: String) : ApiResult<Nothing>
}

val ApiResult<*>.isUnauthorized: Boolean
    get() = this is ApiResult.HttpError && status == 401

val ApiResult<*>.isNotFound: Boolean
    get() = this is ApiResult.HttpError && status == 404

/** Whether the live server answered the last probe. `reachable == null`
 * means it has never been checked. */
data class ServerStatus(
    val reachable: Boolean? = null,
    val lastCheckedAtEpochMs: Long? = null,
    val detail: String? = null,
)

/** Gmail connection for the signed-in user only. `NotConnected` (server
 * answered 404) is a normal state, not an error. */
sealed interface GmailConnection {
    data object Unknown : GmailConnection
    data object NotConnected : GmailConnection
    data class Connected(val email: String) : GmailConnection
}

// ---- Client ----

class ServerApiClient(
    private val auth: FirebaseAuth = Firebase.auth,
    private val baseUrl: String = BuildConfig.RCOS_API_BASE_URL,
    private val timeoutMs: Int = 15_000,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** The signed-in user's Firebase ID token, or null when signed out
     * or the token cannot be minted. Never a token for anyone else:
     * this client only ever runs for the signed-in user. */
    private suspend fun idToken(): String? = try {
        auth.currentUser?.getIdToken(false)?.await()?.token
    } catch (e: Exception) {
        Log.w(TAG, "Could not mint Firebase ID token", e)
        null
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        requireAuth: Boolean,
    ): ApiResult<String> {
        if (requireAuth && auth.currentUser == null) {
            return ApiResult.HttpError(401, "Please sign in to use this feature.")
        }
        val token = if (auth.currentUser != null) idToken() else null
        if (requireAuth && token.isNullOrBlank()) {
            return ApiResult.HttpError(401, "Your sign-in expired. Please sign in again.")
        }
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Accept", "application/json")
                    if (token != null) setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status in 200..299) {
                    ApiResult.Success(text)
                } else {
                    ApiResult.HttpError(status, parseError(text, status))
                }
            } catch (e: IOException) {
                ApiResult.NetworkError(e.localizedMessage ?: "Network request failed.")
            } catch (e: Exception) {
                ApiResult.NetworkError(e.localizedMessage ?: "Network request failed.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    // The server's error shape is {"error": "..."} (sometimes
    // {"message": "..."}). Parsed by hand so a non-JSON error page
    // can never crash error handling itself.
    private fun parseError(body: String, status: Int): String {
        if (body.isBlank()) return "Request failed with status $status"
        val error = Regex("\"error\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.getOrNull(1)
        val message = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.getOrNull(1)
        return error?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: body.take(200)
    }

    private inline fun <reified T> parse(result: ApiResult<String>): ApiResult<T> =
        when (result) {
            is ApiResult.Success -> try {
                ApiResult.Success(json.decodeFromString<T>(result.data))
            } catch (e: Exception) {
                Log.e(TAG, "Could not parse server response", e)
                ApiResult.NetworkError("Server returned a response the app could not read.")
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }

    // ---- Public API (paths copied from RCOS-MOBILE-APP server.ts) ----

    /** GET /api/health — public, no token needed. */
    suspend fun getHealth(): ApiResult<HealthResponse> =
        parse(request("GET", "/api/health", requireAuth = false))

    /** GET /api/firebase-config — public. Lets the app confirm the
     * server's canonical Firestore DB id matches its own. */
    suspend fun getFirebaseConfig(): ApiResult<FirebaseConfigResponse> =
        parse(request("GET", "/api/firebase-config", requireAuth = false))

    /** GET /api/diagnostics/scan — signed-in users only (401 unsigned). */
    suspend fun getDiagnostics(): ApiResult<DiagnosticsScan> =
        parse(request("GET", "/api/diagnostics/scan", requireAuth = true))

    /**
     * POST /api/chat — transport only. The Assistant page UI wiring is
     * Phase 3; this only proves the server round-trip works and returns
     * the real reply (or the server's honest fallback) or an error.
     * `messages` are the app's [ChatMessage] records; roles map exactly
     * as the server expects. The server refuses (403) a businessId the
     * caller is not a member of.
     */
    suspend fun postChat(
        messages: List<ChatMessage>,
        roleId: String = "executive_assistant",
        modelTier: String = "general",
        businessId: String = "",
    ): ApiResult<ChatReply> {
        val requestBody = ChatRequest(
            messages = messages.map {
                ChatApiMessage(
                    role = if (it.role == "assistant" || it.role == "model") "model" else "user",
                    content = it.text,
                )
            },
            roleId = roleId,
            modelTier = modelTier,
            businessId = businessId,
        )
        val encoded = json.encodeToString(ChatRequest.serializer(), requestBody)
        return parse(request("POST", "/api/chat", body = encoded, requireAuth = true))
    }

    /** GET /api/business/{id}/gmail-token — caller's own Gmail token.
     * 404 = Gmail is not connected for this user yet (not an error). */
    suspend fun getGmailToken(businessId: String): ApiResult<GmailTokenResponse> =
        parse(request("GET", "/api/business/${encode(businessId)}/gmail-token", requireAuth = true))

    /** POST /api/business/{id}/gmail/oauth-start — returns the Google
     * OAuth URL to open in the system browser. The OAuth flow itself
     * (deep link back into the app) is Phase 6. */
    suspend fun startGmailOAuth(businessId: String): ApiResult<GmailOAuthStart> =
        parse(request("POST", "/api/business/${encode(businessId)}/gmail/oauth-start", requireAuth = true))
}
