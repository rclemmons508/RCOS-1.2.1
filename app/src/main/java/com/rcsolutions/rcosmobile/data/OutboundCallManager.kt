package com.rcsolutions.rcosmobile.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import com.rcsolutions.rcosmobile.model.OutboundCallStatus
import com.rcsolutions.rcosmobile.model.OutboundCallUiState
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "OutboundCall"

/** No remote audio within this long after joining the room and the
 * call is ended as unanswered — exactly the original app's 50s
 * watchdog (OutboundDialer.tsx). The server has its own 60s ring
 * timeout, so the app always gives up first. */
private const val NO_ANSWER_TIMEOUT_MS = 50_000L

/** The honest message when outbound calling is not set up server-side
 * (trunk missing / 503 `outbound_trunk_not_configured`). Shown instead
 * of a fake connected state. Same wording as the original app's
 * SETUP_MESSAGE (OutboundDialer.tsx). */
const val OUTBOUND_SETUP_MESSAGE =
    "Outbound calling isn't set up yet. Set up the Vonage SIP trunk in LiveKit and set " +
        "LIVEKIT_SIP_OUTBOUND_TRUNK_ID on the server first, then try again."

/** DTMF codes (RFC 4733) — the same table the original app's web
 * client sends with LocalParticipant.publishDtmf. */
private val DTMF_CODES = mapOf(
    "0" to 0, "1" to 1, "2" to 2, "3" to 3, "4" to 4,
    "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9,
    "*" to 10, "#" to 11,
)

/**
 * LiveKit outbound (dial-out) calls — the 1.2.1 port of the original
 * app's OutboundDialer (RCOS-MOBILE-APP PR #75).
 *
 * Flow (same as the original; the server side is unchanged and already
 * live on Railway):
 * 1. POST /api/calls/dial {businessId, to} — the server creates a
 *    LiveKit room, dials the number in as a SIP participant on the
 *    stored Vonage outbound trunk, and returns a publish-capable
 *    token. NO agent is dispatched into an outbound room.
 * 2. Join the room with that token, mic on.
 * 3. Remote audio (the SIP participant's track subscribing) = the
 *    call was answered; the 50s watchdog ends it as "No answer"
 *    otherwise.
 * 4. Hangup POSTs /api/calls/dial/{room}/hangup (best-effort) and
 *    disconnects. The call is logged by the ViewModel only when it
 *    actually connected for > 0 seconds.
 *
 * Audio: earpiece (phone headpiece) by default, exactly like the
 * receptionist live voice — the configure/restore blocks below are
 * the same pattern as [LiveKitVoiceManager]. The speakerphone toggle
 * reports the actual state.
 *
 * The manager owns the call state; the ViewModel mirrors [state] and
 * logs the call when it ends. It never invents a call: every failure
 * surfaces as [OutboundCallStatus.ERROR] with the server's message.
 */
class OutboundCallManager(
    private val appContext: Context,
    private val serverApi: ServerApiClient = ServerApiClient(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(OutboundCallUiState())
    val state: StateFlow<OutboundCallUiState> = _state.asStateFlow()

    private var room: Room? = null
    private var roomName: String? = null
    private var businessId: String? = null
    private var heardRemoteAudio = false
    private var ending = false
    private var eventsJob: Job? = null
    private var watchdogJob: Job? = null
    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var previousAudioMode: Int? = null
    private var previousSpeakerphone: Boolean? = null

    /**
     * Dial [to] in-app over LiveKit SIP. Returns success once the room
     * is joined and the mic is on — the call is not "connected" until
     * remote audio arrives (see [OutboundCallStatus.CONNECTED]). Any
     * failure (bad/blocked number, trunk not configured, gate 403,
     * daily cap) sets [OutboundCallStatus.ERROR] with the server's
     * message and returns it — never a fake connected state.
     */
    suspend fun dial(businessId: String, to: String, displayName: String = ""): Result<Unit> {
        val current = _state.value
        if (current.status == OutboundCallStatus.DIALING ||
            current.status == OutboundCallStatus.CONNECTED
        ) {
            return Result.failure(IllegalStateException("An outbound call is already in progress."))
        }
        resetInternal()
        this.businessId = businessId
        _state.value = OutboundCallUiState(
            status = OutboundCallStatus.DIALING,
            number = to,
            displayName = displayName.ifBlank { to },
        )

        val dialResult = serverApi.dialOutbound(businessId, to)
        val dial = when (dialResult) {
            is ApiResult.Success -> dialResult.data
            is ApiResult.HttpError -> {
                val message = dialResult.message.ifBlank { "The call could not be started." }
                _state.update { it.copy(status = OutboundCallStatus.ERROR, error = message) }
                return Result.failure(IllegalStateException(message))
            }
            is ApiResult.NetworkError -> {
                val message = dialResult.message.ifBlank { "No network connection. The call was not started." }
                _state.update { it.copy(status = OutboundCallStatus.ERROR, error = message) }
                return Result.failure(IllegalStateException(message))
            }
        }

        roomName = dial.room
        return try {
            configureEarpieceAudio()
            toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80) }.getOrNull()
            val newRoom = LiveKit.create(appContext)
            room = newRoom
            eventsJob = scope.launch { collectRoomEvents(newRoom) }
            newRoom.connect(dial.url, dial.token)
            newRoom.localParticipant?.setMicrophoneEnabled(true)
            watchdogJob = scope.launch {
                delay(NO_ANSWER_TIMEOUT_MS)
                if (!heardRemoteAudio) {
                    endCallInternal("No answer — the call was not picked up within 50 seconds.")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Outbound call failed to join the room", e)
            endCallInternal(e.localizedMessage ?: "The call could not be started.")
            Result.failure(e)
        }
    }

    /** Hang up the current call (or do nothing when there is none).
     * The ViewModel logs the call from the state transition. */
    suspend fun hangup() {
        endCallInternal()
    }

    /**
     * Send one DTMF digit to the live call (menus, extensions, PINs).
     * Uses LocalParticipant.publishDtmf with the same RFC 4733 codes
     * as the original app's web client; the local tone plays only
     * after the send succeeds, and a failed send is reported in
     * [OutboundCallUiState.dtmfError] — never silently pretended.
     */
    suspend fun sendDtmf(digit: String) {
        val code = DTMF_CODES[digit]
        val currentRoom = room
        val status = _state.value.status
        if (code == null || currentRoom == null ||
            (status != OutboundCallStatus.DIALING && status != OutboundCallStatus.CONNECTED)
        ) {
            _state.update { it.copy(dtmfError = "No live call — the keypad only works during a call.") }
            return
        }
        runCatching { currentRoom.localParticipant?.publishDtmf(code, digit) }
            .onSuccess {
                playLocalTone(digit)
                _state.update { it.copy(sentDigits = it.sentDigits + digit, dtmfError = null) }
            }
            .onFailure { e ->
                Log.w(TAG, "DTMF send failed", e)
                _state.update {
                    it.copy(dtmfError = "That keypress did not go out — the call may have dropped. Try again, or end the call and redial.")
                }
            }
    }

    /** Mute/unmute the mic. The remote end hears silence while muted. */
    suspend fun setMuted(muted: Boolean) {
        runCatching { room?.localParticipant?.setMicrophoneEnabled(!muted) }
        _state.update { it.copy(muted = muted) }
    }

    /** Flip the speakerphone and return the actual state (the OS can
     * refuse the flip). The call always starts on the earpiece. */
    fun setSpeakerphone(enabled: Boolean): Boolean {
        audioManager.isSpeakerphoneOn = enabled
        val actual = audioManager.isSpeakerphoneOn
        _state.update { it.copy(speakerOn = actual) }
        return actual
    }

    /** Clears the sent-digits record. It cannot unsend a digit that
     * already went out. */
    fun clearSentDigits() {
        _state.update { it.copy(sentDigits = "") }
    }

    /** Clears the ended/error state so the next dial starts clean.
     * Does nothing while a call is in progress. */
    fun reset() {
        val status = _state.value.status
        if (status == OutboundCallStatus.DIALING || status == OutboundCallStatus.CONNECTED) return
        resetInternal()
        _state.value = OutboundCallUiState()
    }

    private fun resetInternal() {
        ending = false
        heardRemoteAudio = false
    }

    private suspend fun collectRoomEvents(room: Room) {
        room.events.collect { event ->
            when (event) {
                is RoomEvent.TrackSubscribed -> {
                    // The SIP participant's audio track subscribing is
                    // the answer: ringing / the other end's voice.
                    // livekit-android plays subscribed remote audio
                    // automatically — no attach step like the web client.
                    if (event.track.kind == Track.Kind.AUDIO) onHeardRemoteAudio()
                }
                is RoomEvent.ParticipantDisconnected -> {
                    // The SIP participant leaving ends the call — the
                    // other end hung up (or the room closed).
                    if (room.remoteParticipants.isEmpty()) endCallInternal()
                }
                is RoomEvent.Disconnected -> endCallInternal()
                else -> Unit
            }
        }
    }

    private fun onHeardRemoteAudio() {
        if (heardRemoteAudio) return
        heardRemoteAudio = true
        watchdogJob?.cancel()
        watchdogJob = null
        _state.update { it.copy(status = OutboundCallStatus.CONNECTED) }
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                _state.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    /**
     * Ends the call and tears everything down. [message] set = the
     * call ended in error (shown to the user); null = a normal hangup.
     * Only the first call wins; the ViewModel logs the call once from
     * the state transition. The number and duration survive in the
     * state so the log has the real values.
     */
    private suspend fun endCallInternal(message: String? = null) {
        if (ending) return
        ending = true
        watchdogJob?.cancel()
        timerJob?.cancel()
        eventsJob?.cancel()
        watchdogJob = null
        timerJob = null
        eventsJob = null
        // Best-effort server hangup: deleting the room drops the SIP
        // leg. A failure here never keeps the local call alive.
        val name = roomName
        val biz = businessId
        if (name != null && biz != null) {
            runCatching { serverApi.hangupOutbound(name, biz) }
        }
        runCatching { room?.disconnect() }
        room = null
        roomName = null
        businessId = null
        restoreAudio()
        toneGenerator?.release()
        toneGenerator = null
        _state.update {
            it.copy(
                status = if (message != null) OutboundCallStatus.ERROR else OutboundCallStatus.ENDED,
                error = message,
                speakerOn = false,
                muted = false,
            )
        }
    }

    private fun playLocalTone(digit: String) {
        val tone = when (digit) {
            "0" -> ToneGenerator.TONE_DTMF_0
            "1" -> ToneGenerator.TONE_DTMF_1
            "2" -> ToneGenerator.TONE_DTMF_2
            "3" -> ToneGenerator.TONE_DTMF_3
            "4" -> ToneGenerator.TONE_DTMF_4
            "5" -> ToneGenerator.TONE_DTMF_5
            "6" -> ToneGenerator.TONE_DTMF_6
            "7" -> ToneGenerator.TONE_DTMF_7
            "8" -> ToneGenerator.TONE_DTMF_8
            "9" -> ToneGenerator.TONE_DTMF_9
            "*" -> ToneGenerator.TONE_DTMF_S
            "#" -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        runCatching { toneGenerator?.startTone(tone, 120) }
    }

    /** Earpiece (phone headpiece) before any audio plays — same pattern
     * as LiveKitVoiceManager.configureEarpieceAudio. The call never
     * starts on the loudspeaker. */
    private fun configureEarpieceAudio() {
        previousAudioMode = audioManager.mode
        previousSpeakerphone = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    /** Same restore as LiveKitVoiceManager.stopConversation. */
    private fun restoreAudio() {
        audioManager.isSpeakerphoneOn = previousSpeakerphone ?: false
        audioManager.mode = previousAudioMode ?: AudioManager.MODE_NORMAL
        previousAudioMode = null
        previousSpeakerphone = null
        audioManager.abandonAudioFocus(null)
    }
}
