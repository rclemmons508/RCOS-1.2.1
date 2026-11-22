package com.rcsolutions.rcosmobile.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "LiveVoiceManager"
const val MODEL_GEMINI_LIVE = "gemini-3.8-live"

@OptIn(PublicPreviewAPI::class)
class LiveVoiceManager {

    private var activeSession: LiveSession? = null

    val isConnected: Boolean
        get() = activeSession != null

    suspend fun startConversation(
        systemInstructionText: String,
        voiceName: String = "Aoede"
    ): Result<Unit> {
        stopConversation()
        return try {
            val liveModel = Firebase.ai.liveModel(
                modelName = MODEL_GEMINI_LIVE,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice(voiceName))
                },
                systemInstruction = content {
                    text(systemInstructionText)
                }
            )
            val session = withTimeout(30_000L) {
                liveModel.connect()
            }
            activeSession = session
            session.startAudioConversation()
            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini Live connection timed out", e)
            stopConversation()
            Result.failure(Exception("Voice connection timed out after 30 seconds. Please retry."))
        } catch (e: CancellationException) {
            stopConversation()
            throw e
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission missing for Gemini Live", e)
            stopConversation()
            Result.failure(Exception("Microphone permission is required for real-time voice conversations."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Gemini Live voice session", e)
            stopConversation()
            Result.failure(e)
        }
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
        runCatching { session.stopAudioConversation() }
            .onFailure { Log.w(TAG, "Error stopping audio conversation", it) }
        runCatching { session.close() }
            .onFailure { Log.w(TAG, "Error closing live session", it) }
    }
}
