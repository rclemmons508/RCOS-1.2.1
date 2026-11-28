package com.rcsolutions.rcosmobile.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.rcsolutions.rcosmobile.BuildConfig
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
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
 *
 * Phase 3 adds `POST /api/voice/token` and the assistant-documents
 * endpoints (`POST` / `GET` / `DELETE /api/assistant/documents`).
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
    val documentIds: List<String> = emptyList(),
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

/** POST /api/voice/token request. `persona` is one of the server's
 * personas: receptionist | sales | operations | marketing | support |
 * executive | technician | finance. */
@Serializable
data class VoiceTokenRequest(
    val businessId: String,
    val persona: String,
)

/** POST /api/voice/token response. The server creates the room,
 * dispatches the `rcos-receptionist` agent, and returns `{token, url,
 * room}`. 503 when LiveKit is not configured server-side. */
@Serializable
data class VoiceTokenResponse(
    val token: String = "",
    val url: String = "",
    val room: String = "",
)

/** POST /api/calls/dial request — same shape the original app's
 * `dialNumber` sends (`src/lib/outboundCalls.ts`). */
@Serializable
data class DialOutboundRequest(
    val businessId: String,
    val to: String,
)

/** A body that carries only a businessId (hangup). */
@Serializable
data class BusinessIdRequest(
    val businessId: String,
)

/** POST /api/calls/dial response. The server creates the room, dials
 * the number in as a SIP participant on the stored Vonage outbound
 * trunk, and returns a publish-capable token. NO agent is dispatched
 * into an outbound room. 503 = the outbound trunk is not configured;
 * 400 = bad/blocked destination; 429 = daily cap. */
@Serializable
data class DialResponse(
    val token: String = "",
    val url: String = "",
    val room: String = "",
    val to: String = "",
)

/** GET /api/calls/outbound-status response — whether the server can
 * dial out at all. */
@Serializable
data class OutboundStatus(
    val configured: Boolean = false,
    val livekitConfigured: Boolean = false,
    val trunkConfigured: Boolean = false,
)

/** Details the server extracted from an uploaded document
 * (`POST /api/assistant/documents`). `found` lists which fields were
 * found (a list of field names, not a boolean). Text is never sent to
 * the list endpoint. */
@Serializable
data class ExtractedDetails(
    val companyName: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val found: List<String> = emptyList(),
    val source: String = "",
)

/** One file uploaded to the Assistant page
 * (`POST /api/assistant/documents` → 201). `uploadedAt` is the
 * server's ISO timestamp string. */
@Serializable
data class AssistantDocument(
    val id: String = "",
    val businessId: String = "",
    val name: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val textChars: Int = 0,
    val textTruncated: Boolean = false,
    val extracted: ExtractedDetails = ExtractedDetails(),
    val uploadedBy: String = "",
    val uploadedAt: String = "",
)

/** POST /api/assistant/documents request (old app's client sends
 * exactly these four fields). */
@Serializable
data class UploadAssistantDocumentRequest(
    val businessId: String,
    val name: String,
    val mimeType: String,
    val fileBase64: String,
)

/** One member of a business roster, as the server stores it
 * (`sanitizeMembers` in server.ts): userId, email, name, role
 * ('owner' | 'member'), joinedAt. */
@Serializable
data class BusinessSyncMember(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "member",
    val joinedAt: String = "",
)

/** Contact block the server reads from a business
 * (`documentContactInfo` coercion in server.ts keeps phone/email). */
@Serializable
data class BusinessSyncContactInfo(
    val email: String = "",
    val phone: String = "",
)

/** POST /api/businesses request — the full business record, as the
 * old app's onboarding sends it. The server coerces each field and
 * ignores unknown ones; an owner re-saving their own business is
 * allowed (200), a new one is created (201), 409 only when someone
 * else owns that id. Only the fields the server reads are sent. */
@Serializable
data class BusinessSyncRequest(
    val id: String,
    val ownerId: String = "",
    val members: List<BusinessSyncMember> = emptyList(),
    val contactEmail: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val name: String = "",
    val industry: String = "general_small_business",
    val size: String = "",
    val services: List<String> = emptyList(),
    val pricingApproach: String = "",
    val brandTone: String = "",
    val businessKnowledge: String = "",
    val painPoints: List<String> = emptyList(),
    val autonomyMode: String = "autonomous",
    val answeringMode: String = "ai",
    val dollarThreshold: Double = 250.0,
    val activeEmployees: Map<String, Boolean> = emptyMap(),
    val onboardingCompleted: Boolean = false,
    val serviceArea: String = "",
    val businessHours: String = "",
    val emergencyAvailable: Boolean = false,
    val teamSize: String = "",
    val servesResidential: Boolean = false,
    val servesCommercial: Boolean = false,
    val documentContactInfo: BusinessSyncContactInfo? = null,
)

/** One row of GET /api/businesses — the server returns only the
 * caller's own businesses. Only the id is needed (to check whether
 * the server already has this business); the rest is ignored. */
@Serializable
data class ServerBusinessSummary(
    val id: String = "",
)

/**
 * POST /api/push/register — register this device's FCM token so the
 * old Railway server sends the receptionist-answered alert to THIS
 * app. Same contract as the old app's registerPushToken
 * (`src/lib/pushNotifications.ts`): the server writes the token doc
 * to Firestore `pushTokens` (default DB) with the same shape
 * (`{token, businessId, userId, email, platform, updatedAt}`, doc id
 * = sha256 of the token). No server change needed.
 */
@Serializable
data class PushRegisterRequest(
    val token: String,
    val platform: String = "android",
    val businessId: String,
)

/** GET /api/calls/live — one live call, from the server's in-memory
 * registry (server/liveCalls.ts). The old app polled this every 6s
 * (`src/lib/liveCalls.ts`). `startedAt` is an ISO string. */
@Serializable
data class LiveCall(
    val roomName: String = "",
    val roomSid: String = "",
    val callerNumber: String = "",
    val calledNumber: String = "",
    val callId: String = "",
    val startedAt: String = "",
    val status: String = "",
    val businessId: String = "",
)

/** GET /api/phone/calls — one finished call from the server's own
 * call log (server/inboundCallLog.ts). This is the ONLY record of
 * receptionist-answered calls: the server writes them to its local
 * store, not to Firestore, which is why they never appeared in this
 * app's Firestore-only log. `timestamp` is an ISO string;
 * `duration` is seconds. */
@Serializable
data class ServerPhoneCall(
    val id: String = "",
    val callerName: String = "",
    val callerNumber: String = "",
    val type: String = "",
    val status: String = "",
    val timestamp: String = "",
    val duration: Int = 0,
    val summary: String = "",
    val answeredBy: String = "",
)

/**
 * Build the POST /api/businesses payload from the Firestore business
 * record. The gate proves membership from the verified Firebase
 * token, so the payload must name the SAME owner: ownerId is the
 * record's owner (falling back to the signed-in user), and the
 * members roster is guaranteed to contain an owner entry for the
 * signed-in user — a record whose roster lost it (older docs) would
 * otherwise 403 every gated call even though the owner is right.
 * contactEmail falls back through the document contact block and the
 * owner's own email, because the live business keeps its email in
 * documentContactInfo, not top-level contactEmail.
 *
 * Audit Phase 1 (finding 1) — the owner entry is synthesized ONLY
 * when the current user IS the owner (or the record is ownerless,
 * preserving the adopt repair: ownerId blank falls back to the
 * current user). A non-owner member used to get an "owner" entry
 * with the real owner's uid but the member's own email/name, which
 * the server then stored. The re-sync itself is also gated on the
 * owner in MainViewModel.syncBusinessToServer; this is the second
 * layer, so the mapper can never fabricate the entry on its own.
 */
fun BusinessAccount.toBusinessSyncRequest(
    currentUid: String,
    currentEmail: String,
    currentName: String,
): BusinessSyncRequest {
    val ownerUid = ownerId.ifBlank { currentUid }
    val ownerEmail = currentEmail.trim().lowercase()
    val roster = members.map {
        BusinessSyncMember(
            userId = it.userId,
            email = it.email,
            name = it.name,
            role = if (it.role == "owner") "owner" else "member",
        )
    }.filter { it.userId.isNotBlank() || it.email.isNotBlank() }.toMutableList()
    val hasOwnerEntry = roster.any {
        it.role == "owner" && (it.userId == ownerUid ||
            (ownerEmail.isNotBlank() && it.email.trim().lowercase() == ownerEmail))
    }
    // Audit Phase 1 (finding 1) — synthesize the owner entry only
    // when the current user IS the owner (ownerUid == currentUid
    // covers the ownerless-record adopt fallback, where ownerUid
    // already fell back to currentUid). A non-owner never gets a
    // fabricated owner entry carrying their own email/name.
    if (!hasOwnerEntry && ownerUid.isNotBlank() && ownerUid == currentUid) {
        roster.add(
            BusinessSyncMember(
                userId = ownerUid,
                email = ownerEmail,
                name = currentName.ifBlank { contactName.ifBlank { name } },
                role = "owner",
            )
        )
    }
    val contactEmailValue = contactEmail
        .ifBlank { documentContactInfo?.email.orEmpty() }
        .ifBlank { ownerEmail }
    return BusinessSyncRequest(
        id = id,
        ownerId = ownerUid,
        members = roster,
        contactEmail = contactEmailValue,
        contactName = contactName.ifBlank { currentName },
        contactPhone = contactPhone.ifBlank { documentContactInfo?.phone.orEmpty() },
        name = name.ifBlank { "My Business" },
        industry = industry.ifBlank { "general_small_business" },
        size = size,
        services = services,
        pricingApproach = pricingApproach,
        brandTone = brandTone,
        businessKnowledge = businessKnowledge,
        painPoints = painPoints,
        autonomyMode = autonomyMode,
        answeringMode = if (answeringMode == "manual") "manual" else "ai",
        dollarThreshold = dollarThreshold,
        activeEmployees = activeEmployees,
        onboardingCompleted = onboardingCompleted,
        serviceArea = serviceArea,
        businessHours = businessHours,
        emergencyAvailable = emergencyAvailable,
        teamSize = teamSize,
        servesResidential = servesResidential,
        servesCommercial = servesCommercial,
        documentContactInfo = documentContactInfo?.let {
            BusinessSyncContactInfo(email = it.email, phone = it.phone)
        },
    )
}

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
     * this client only ever runs for the signed-in user. Audit Phase 1
     * (finding 5) — `forceRefresh` mints a fresh token; used for the
     * single retry after a 401, when the cached token may have
     * expired between minting and the call. */
    private suspend fun idToken(forceRefresh: Boolean = false): String? = try {
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
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
        timeoutMsOverride: Int? = null,
    ): ApiResult<String> {
        if (requireAuth && auth.currentUser == null) {
            return ApiResult.HttpError(401, "Please sign in to use this feature.")
        }
        val token = if (auth.currentUser != null) idToken() else null
        if (requireAuth && token.isNullOrBlank()) {
            return ApiResult.HttpError(401, "Your sign-in expired. Please sign in again.")
        }
        val first = executeRequest(method, path, body, token, timeoutMsOverride)
        // Audit Phase 1 (finding 5) — a cached ID token can expire
        // between minting and the call. On a 401, mint a fresh token
        // once and retry the request exactly once (no loops). A 401
        // after the retry is a real auth failure and is returned
        // honestly. A 401 before the token existed is impossible here
        // (early returns above); a 401 on a public call is the
        // server's business, not the token's, so it is not retried.
        if (requireAuth && token != null &&
            first is ApiResult.HttpError && first.status == 401
        ) {
            val freshToken = idToken(forceRefresh = true)
            if (!freshToken.isNullOrBlank()) {
                return executeRequest(method, path, body, freshToken, timeoutMsOverride)
            }
        }
        return first
    }

    /** One HTTP round trip with the given token. Split out of
     * [request] so a 401 can retry it with a fresh token. */
    private suspend fun executeRequest(
        method: String,
        path: String,
        body: String?,
        token: String?,
        timeoutMsOverride: Int?,
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // Audit Phase 2 (finding 10) — the blocking
                // HttpURLConnection calls run in runInterruptible, so
                // a cancelled scope interrupts the thread instead of
                // letting the 6 s call polling run on after the caller
                // leaves Calls. Cancellation is never swallowed as a
                // network error (see the catches below).
                runInterruptible {
                    // Local val, not the captured var: a smart cast
                    // on the outer var would not survive the lambda
                    // boundary. The outer var is only for the
                    // finally block's disconnect().
                    val conn = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                        requestMethod = method
                        connectTimeout = timeoutMsOverride ?: timeoutMs
                        readTimeout = timeoutMsOverride ?: timeoutMs
                        setRequestProperty("Accept", "application/json")
                        if (token != null) setRequestProperty("Authorization", "Bearer $token")
                        if (body != null) {
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                            // Audit Phase 2 (finding 7) — without a
                            // streaming mode HttpURLConnection buffers
                            // the whole request body in memory before
                            // sending. A 10 MB document upload is
                            // ~14 MB of base64 JSON; fixed-length mode
                            // lets the connection stream it instead.
                            val bodyBytes = body.toByteArray(Charsets.UTF_8)
                            setFixedLengthStreamingMode(bodyBytes.size)
                            outputStream.use { it.write(bodyBytes) }
                        }
                    }
                    connection = conn
                    val status = conn.responseCode
                    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (status in 200..299) {
                        ApiResult.Success(text)
                    } else {
                        ApiResult.HttpError(status, parseError(text, status))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // A cancelled blocking read surfaces as an
                // InterruptedIOException once the thread is
                // interrupted — rethrow it as a cancellation instead
                // of reporting a network error.
                ensureActive()
                ApiResult.NetworkError(e.localizedMessage ?: "Network request failed.")
            } catch (e: Exception) {
                ensureActive()
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
     * POST /api/chat — wired to the Assistant page in Phase 3. Returns
     * the real reply (or the server's honest fallback) or an error.
     * `messages` are the app's [ChatMessage] records; roles map exactly
     * as the server expects. The server refuses (403) a businessId the
     * caller is not a member of. `documentIds` are server-side
     * assistant documents the server injects as grounded context (it
     * never invents details from them).
     */
    suspend fun postChat(
        messages: List<ChatMessage>,
        roleId: String = "executive_assistant",
        modelTier: String = "general",
        businessId: String = "",
        documentIds: List<String> = emptyList(),
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
            documentIds = documentIds,
        )
        val encoded = json.encodeToString(ChatRequest.serializer(), requestBody)
        // Audit Phase 2 (finding 6) — chat shares the 15 s default
        // timeout, and LLM replies often exceed it, so a working
        // request looked like a network error. 60 s, same as document
        // uploads. The 15 s default is unchanged for other calls.
        return parse(request("POST", "/api/chat", body = encoded, requireAuth = true, timeoutMsOverride = 60_000))
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

    /** POST /api/voice/token — LiveKit token for a live voice room.
     * `persona` must be one of the server's personas (see
     * [VoiceTokenRequest]). 503 = LiveKit is not configured on the
     * server; 401 = sign-in expired. A token with a blank `token` or
     * `url` is treated as a failure, never passed to LiveKit. */
    suspend fun requestVoiceToken(businessId: String, persona: String): ApiResult<VoiceTokenResponse> {
        val encoded = json.encodeToString(
            VoiceTokenRequest.serializer(),
            VoiceTokenRequest(businessId = businessId, persona = persona),
        )
        return when (val result = parse<VoiceTokenResponse>(request("POST", "/api/voice/token", body = encoded, requireAuth = true))) {
            is ApiResult.Success -> if (result.data.token.isBlank() || result.data.url.isBlank()) {
                ApiResult.HttpError(502, "Voice service returned an incomplete token.")
            } else {
                result
            }
            else -> result
        }
    }

    /** POST /api/assistant/documents — upload one document (PDF, Word,
     * text, CSV; 10 MB max — the server answers 422 when it is too big
     * or has no readable text). Extraction can take ~40s, so this call
     * gets a 60s timeout like the old app's client. The server returns
     * the 201 document record with extracted details. */
    suspend fun uploadAssistantDocument(
        businessId: String,
        name: String,
        mimeType: String,
        fileBase64: String,
    ): ApiResult<AssistantDocument> {
        val encoded = json.encodeToString(
            UploadAssistantDocumentRequest.serializer(),
            UploadAssistantDocumentRequest(
                businessId = businessId,
                name = name,
                mimeType = mimeType,
                fileBase64 = fileBase64,
            ),
        )
        return parse(request("POST", "/api/assistant/documents", body = encoded, requireAuth = true, timeoutMsOverride = 60_000))
    }

    /** GET /api/assistant/documents — this business's documents, newest
     * first. The server returns a bare JSON array (text is never
     * included). */
    suspend fun listAssistantDocuments(businessId: String): ApiResult<List<AssistantDocument>> =
        parse(request("GET", "/api/assistant/documents?businessId=${encode(businessId)}", requireAuth = true))

    /** GET /api/businesses — the caller's own businesses only (the
     * server never returns another tenant's). Used by the re-sync to
     * skip the POST when the server already has this business. */
    suspend fun listServerBusinesses(): ApiResult<List<ServerBusinessSummary>> =
        parse(request("GET", "/api/businesses", requireAuth = true))

    /** POST /api/businesses — (re-)save the full business record on
     * the server so the login gate passes after a redeploy wipes the
     * server's store. An owner re-saving their own business answers
     * 200; a new record answers 201; 409 only when someone else owns
     * that id. Any 2xx counts as saved — the body is not parsed. */
    suspend fun registerBusiness(business: BusinessSyncRequest): ApiResult<Unit> {
        val encoded = json.encodeToString(
            BusinessSyncRequest.serializer(),
            business,
        )
        return when (val result = request("POST", "/api/businesses", body = encoded, requireAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }
    }

    /** DELETE /api/assistant/documents/{id} — the server answers 2xx
     * with no body the client needs; any 2xx counts as deleted. */
    suspend fun deleteAssistantDocument(businessId: String, documentId: String): ApiResult<Unit> =
        when (val result = request("DELETE", "/api/assistant/documents/${encode(documentId)}?businessId=${encode(businessId)}", requireAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }

    /** GET /api/calls/outbound-status — whether outbound dialing is
     * configured on the server. Unknown status never blocks a dial
     * attempt: the dial itself surfaces the real error. */
    suspend fun fetchOutboundStatus(businessId: String): ApiResult<OutboundStatus> =
        parse(request("GET", "/api/calls/outbound-status?businessId=${encode(businessId)}", requireAuth = true))

    /** POST /api/push/register — (re-)register this device's FCM
     * token for the active business. Re-sent on every launch even if
     * the token is unchanged (same as the old app), so a server
     * redeploy that wipes the token store never strands the device.
     * Any 2xx counts as registered; the body is not parsed. */
    suspend fun registerPushToken(businessId: String, token: String): ApiResult<Unit> {
        val encoded = json.encodeToString(
            PushRegisterRequest.serializer(),
            PushRegisterRequest(token = token, businessId = businessId),
        )
        return when (val result = request("POST", "/api/push/register", body = encoded, requireAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }
    }

    /** GET /api/calls/live — the calls the receptionist is handling
     * RIGHT NOW. Polled every 6s while the Calls screen is visible
     * (same cadence as the old app). An empty list means no live
     * call; a failure means the server could not be reached and is
     * surfaced by the caller, never faked as an empty list. */
    suspend fun fetchLiveCalls(businessId: String): ApiResult<List<LiveCall>> =
        parse(request("GET", "/api/calls/live?businessId=${encode(businessId)}", requireAuth = true))

    /** GET /api/phone/calls — the server's finished-call log. This is
     * the only place receptionist-answered calls are recorded, so the
     * app merges this with its Firestore log. The server answers a
     * bare JSON array. */
    suspend fun fetchServerCallLog(businessId: String): ApiResult<List<ServerPhoneCall>> =
        parse(request("GET", "/api/phone/calls?businessId=${encode(businessId)}", requireAuth = true))

    /** POST /api/calls/dial — start an outbound call. 30s timeout:
     * the server creates the room and places the SIP call before it
     * answers (same as the original app's `dialNumber`). A blank
     * token/url/room is treated as a failure, never passed to LiveKit. */
    suspend fun dialOutbound(businessId: String, to: String): ApiResult<DialResponse> {
        val encoded = json.encodeToString(
            DialOutboundRequest.serializer(),
            DialOutboundRequest(businessId = businessId, to = to),
        )
        return when (val result = parse<DialResponse>(request("POST", "/api/calls/dial", body = encoded, requireAuth = true, timeoutMsOverride = 30_000))) {
            is ApiResult.Success -> if (result.data.token.isBlank() || result.data.url.isBlank() || result.data.room.isBlank()) {
                ApiResult.HttpError(502, "The server answered but the call could not be started. Try again.")
            } else {
                result
            }
            else -> result
        }
    }

    /** POST /api/calls/dial/{room}/hangup — deletes the room, which
     * drops the SIP leg. Best-effort: any result counts as hung up
     * (the room may already be gone). The original app's
     * `hangupOutboundCall` sends the businessId in the body. */
    suspend fun hangupOutbound(roomName: String, businessId: String): ApiResult<Unit> {
        val encoded = json.encodeToString(
            BusinessIdRequest.serializer(),
            BusinessIdRequest(businessId = businessId),
        )
        return when (val result = request("POST", "/api/calls/dial/${encode(roomName)}/hangup", body = encoded, requireAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
        }
    }

}
