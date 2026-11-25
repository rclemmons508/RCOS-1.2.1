package com.rcsolutions.rcosmobile.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.ServiceConnectionHandshakeFailedException
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "LiveVoiceManager"
const val MODEL_GEMINI_LIVE = "gemini-3.8-live"
const val MODEL_GEMINI_LIVE_FALLBACK = "gemini-3.1-flash-live-preview"

@OptIn(PublicPreviewAPI::class)
class LiveVoiceManager {

    private var activeSession: LiveSession? = null

    val isConnected: Boolean
        get() = activeSession != null

    private data class LiveAttemptConfig(
        val modelName: String,
        val voiceName: String?,
        val includeSystemInstruction: Boolean
    )

    suspend fun startConversation(
        systemInstructionText: String,
        voiceName: String = "Aoede"
    ): Result<String> {
        stopConversation()
        val attempts = listOf(
            LiveAttemptConfig(MODEL_GEMINI_LIVE, voiceName, includeSystemInstruction = true),
            LiveAttemptConfig(MODEL_GEMINI_LIVE, voiceName = null, includeSystemInstruction = true),
            LiveAttemptConfig(MODEL_GEMINI_LIVE, voiceName = null, includeSystemInstruction = false),
            LiveAttemptConfig(MODEL_GEMINI_LIVE_FALLBACK, voiceName = null, includeSystemInstruction = true)
        )

        var lastError: Throwable? = null
        val backend = Firebase.ai(backend = GenerativeBackend.googleAI())

        for (attempt in attempts) {
            var pendingSession: LiveSession? = null
            try {
                val liveModel = backend.liveModel(
                    modelName = attempt.modelName,
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.AUDIO
                        if (!attempt.voiceName.isNullOrBlank()) {
                            speechConfig = SpeechConfig(voice = Voice(attempt.voiceName))
                        }
                    },
                    systemInstruction = if (attempt.includeSystemInstruction && systemInstructionText.isNotBlank()) {
                        content { text(systemInstructionText) }
                    } else {
                        null
                    }
                )
                val session = withTimeout(20_000L) {
                    liveModel.connect()
                }
                pendingSession = session
                session.startAudioConversation()
                activeSession = session
                Log.i(TAG, "Gemini Live session connected on model ${attempt.modelName}")
                return Result.success(attempt.modelName)
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    Log.w(TAG, "Gemini Live handshake timed out on ${attempt.modelName}", e)
                    cleanupSessionQuietly(pendingSession)
                    lastError = e
                } else {
                    cleanupSessionQuietly(pendingSession)
                    stopConversation()
                    throw e
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission missing for Gemini Live", e)
                cleanupSessionQuietly(pendingSession)
                stopConversation()
                return Result.failure(
                    Exception("Microphone permission is required for real-time voice conversations.")
                )
            } catch (e: ServiceConnectionHandshakeFailedException) {
                Log.w(TAG, "Gemini Live WebSocket handshake rejected on ${attempt.modelName}: ${e.message}", e)
                cleanupSessionQuietly(pendingSession)
                lastError = e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Live attempt failed on ${attempt.modelName}: ${e.message}", e)
                cleanupSessionQuietly(pendingSession)
                lastError = e
            }
        }

        stopConversation()
        val finalException = lastError
        Log.e(TAG, "Failed to start Gemini Live voice session across all models", finalException)
        val userMessage = when (finalException) {
            is TimeoutCancellationException ->
                "Voice connection timed out. Check your signal and tap Voice to retry."
            is ServiceConnectionHandshakeFailedException ->
                "Could not establish the Gemini Live audio channel. Verify your internet connection and try again."
            else ->
                finalException?.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Gemini Live voice session could not connect. Please try again."
        }
        return Result.failure(Exception(userMessage, finalException))
    }

    suspend fun sendTextNudge(prompt: String): Result<Unit> {
        val session = activeSession ?: return Result.failure(IllegalStateException("No active live voice session."))
        return try {
            withTimeout(15_000L) {
                session.send(prompt)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send prompt to active live session", e)
            Result.failure(e)
        }
    }

    suspend fun stopConversation() {
        val session = activeSession ?: return
        activeSession = null
        cleanupSessionQuietly(session)
    }

    private suspend fun cleanupSessionQuietly(session: LiveSession?) {
        if (session == null) return
        runCatching { session.stopAudioConversation() }
            .onFailure { Log.w(TAG, "Error stopping audio conversation", it) }
        runCatching { session.close() }
            .onFailure { Log.w(TAG, "Error closing live session", it) }
    }
}
