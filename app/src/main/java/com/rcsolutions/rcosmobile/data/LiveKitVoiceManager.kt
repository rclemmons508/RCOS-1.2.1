package com.rcsolutions.rcosmobile.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "LiveKitVoice"

/** Label shown while a LiveKit voice session is live. The room itself
 * is the transport; the agent (Gemini realtime, same engine as the
 * phone receptionist) runs in the server's `rcos-voice-agent` worker. */
const val MODEL_LIVEKIT_VOICE = "LiveKit"

/**
 * Phase 3 — in-app live voice over LiveKit (replaces the direct
 * Firebase AI Live session in the deleted `LiveVoiceManager`).
 *
 * Flow: mint a room token from the server (`POST /api/voice/token`,
 * which creates the room and dispatches the `rcos-receptionist`
 * agent) → join the room → mic on → wait up to 25s for the agent to
 * join. Audio starts on the normal earpiece (phone headpiece), never
 * the speakerphone: `AudioManager.MODE_IN_COMMUNICATION` with
 * `isSpeakerphoneOn = false`. The speakerphone toggle only flips
 * `isSpeakerphoneOn` and reports the actual state back.
 *
 * Nothing here fakes a session: if the token call fails, the join
 * fails, or the agent never joins, [startConversation] returns a
 * failed [Result] with the honest reason and no room stays open.
 */
class LiveKitVoiceManager(
    private val appContext: Context,
    private val serverApi: ServerApiClient = ServerApiClient(),
) {
    private var room: Room? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var previousSpeakerphone: Boolean = false

    /**
     * Join a LiveKit room for `persona` (server persona names:
     * receptionist | sales | operations | marketing | support |
     * executive | technician | finance). Returns the room name on
     * success. The agent must join within 25s or the attempt fails
     * honestly and the room is closed.
     */
    suspend fun startConversation(businessId: String, persona: String): Result<String> {
        if (room != null) return Result.failure(IllegalStateException("A live voice session is already open."))
        val voiceToken = when (val tokenResult = serverApi.requestVoiceToken(businessId, persona)) {
            is ApiResult.Success -> tokenResult.data
            is ApiResult.HttpError -> return Result.failure(
                Exception("Could not start live voice (${tokenResult.status}): ${tokenResult.message}")
            )
            is ApiResult.NetworkError -> return Result.failure(
                Exception("Server unreachable - check your connection and try again.")
            )
        }
        configureEarpieceAudio()
        return try {
            val newRoom = LiveKit.create(appContext)
            room = newRoom
            newRoom.connect(voiceToken.url, voiceToken.token)
            newRoom.localParticipant?.setMicrophoneEnabled(true)
            if (!waitForAgent(newRoom)) {
                stopConversation()
                return Result.failure(Exception("The AI voice agent did not join within 25 seconds. Try again."))
            }
            Log.i(TAG, "LiveKit room joined: ${voiceToken.room}")
            Result.success(voiceToken.room)
        } catch (e: Exception) {
            Log.e(TAG, "LiveKit join failed", e)
            stopConversation()
            Result.failure(Exception(e.localizedMessage ?: "Could not join the live voice room."))
        }
    }

    /** True when the agent is in the room — polled from
     * `remoteParticipants` (so a join that fires before we start
     * listening cannot be missed), up to 25s. */
    private suspend fun waitForAgent(room: Room): Boolean {
        if (room.remoteParticipants.isNotEmpty()) return true
        return withTimeoutOrNull(25_000L) {
            while (room.remoteParticipants.isEmpty()) {
                delay(500L)
            }
            true
        } ?: false
    }

    /**
     * Flip the speakerphone and return the ACTUAL state afterwards
     * (false when no session is open). The session always starts on
     * the earpiece; this is the only way onto the speaker.
     */
    fun setSpeakerphone(enabled: Boolean): Boolean {
        val manager = audioManager ?: return false
        manager.isSpeakerphoneOn = enabled
        return manager.isSpeakerphoneOn
    }

    /** Leave the room and restore the audio routing that was active
     * before the session started. Safe to call when no session is open. */
    suspend fun stopConversation() {
        try {
            room?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "LiveKit disconnect failed", e)
        } finally {
            room = null
            restoreAudio()
        }
    }

    /** Earpiece routing: voice-call audio mode, speakerphone off,
     * transient voice-communication focus. Remembers the previous
     * mode/speaker state so [stopConversation] can restore it. */
    private fun configureEarpieceAudio() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager
        previousAudioMode = manager.mode
        previousSpeakerphone = manager.isSpeakerphoneOn
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        manager.isSpeakerphoneOn = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun restoreAudio() {
        val manager = audioManager ?: return
        audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        manager.isSpeakerphoneOn = previousSpeakerphone
        manager.mode = previousAudioMode
        audioManager = null
    }
}
