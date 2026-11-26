package com.rcsolutions.rcosmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcsolutions.rcosmobile.model.OutboundCallStatus
import com.rcsolutions.rcosmobile.model.OutboundCallUiState
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraCanvasDeep
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary

/** DTMF keys with their dialpad letters — the same layout as the
 * original app's DtmfKeypad (and the Calls dialpad). */
private val DTMF_KEYS: List<Pair<String, String>> = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to "",
)

/**
 * Full-screen in-call UI for an outbound LiveKit call — the 1.2.1
 * version of the original app's OutboundDialer in-call view. Rendered
 * above the Scaffold (covers the bottom nav too) while a call is
 * dialing or connected. Earpiece by default; mute, speakerphone, and
 * the DTMF keypad are all on this screen. The call is ended only from
 * the End button — there is no back-press shortcut that could drop a
 * live call by accident.
 */
@Composable
fun OutboundCallOverlay(
    state: OutboundCallUiState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onDigit: (String) -> Unit,
    onClearSentDigits: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeypad by remember { mutableStateOf(false) }
    val connected = state.status == OutboundCallStatus.CONNECTED
    val statusText = if (connected) "On the call" else "Calling…"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraCanvasDeep)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.displayName.ifBlank { state.number },
                style = MaterialTheme.typography.headlineMedium,
                color = AuroraTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (state.displayName.isNotBlank() && state.displayName != state.number) {
                Text(
                    text = state.number,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AuroraTextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCallClock(state.durationSeconds),
                style = MaterialTheme.typography.headlineSmall,
                color = AuroraTextPrimary,
                modifier = Modifier.testTag("outbound_call_timer")
            )
            Spacer(Modifier.height(28.dp))
            // Avatar circle (emerald, like the original's pulsing
            // green circle while ringing).
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(AuroraEmerald.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(AuroraEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = AuroraOnEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                OutboundCallControlButton(
                    label = if (state.muted) "Unmute" else "Mute",
                    icon = if (state.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                    active = state.muted,
                    onClick = onToggleMute
                )
                OutboundCallControlButton(
                    label = "Speaker",
                    icon = Icons.Outlined.VolumeUp,
                    active = state.speakerOn,
                    onClick = onToggleSpeakerphone
                )
                OutboundCallControlButton(
                    label = "Keypad",
                    icon = Icons.Outlined.Dialpad,
                    active = showKeypad,
                    onClick = { showKeypad = !showKeypad }
                )
            }
            if (showKeypad) {
                Spacer(Modifier.height(20.dp))
                OutboundDtmfKeypad(
                    sentDigits = state.sentDigits,
                    dtmfError = state.dtmfError,
                    onDigit = onDigit,
                    onClear = onClearSentDigits
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onEndCall,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraRed,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(72.dp).testTag("outbound_end_call_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CallEnd,
                    contentDescription = "End call",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "End call",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary
            )
        }
    }
}

@Composable
private fun OutboundCallControlButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) AuroraEmerald else AuroraCardElevated)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) AuroraOnEmerald else AuroraTextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AuroraTextSecondary
        )
    }
}

/** In-call DTMF keypad — the original app's DtmfKeypad, in Compose.
 * Digits are sent as they are pressed; the record shows what already
 * went out (Clear only clears the record, it cannot unsend a digit). */
@Composable
private fun OutboundDtmfKeypad(
    sentDigits: String,
    dtmfError: String?,
    onDigit: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuroraCardElevated)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = sentDigits.ifBlank { "Digits sent will show here" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sentDigits.isBlank()) AuroraTextMuted else AuroraTextPrimary
                )
            }
            IconButton(
                onClick = onClear,
                enabled = sentDigits.isNotEmpty(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backspace,
                    contentDescription = "Clear sent digits",
                    tint = AuroraTextSecondary
                )
            }
        }
        DTMF_KEYS.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    Button(
                        onClick = { onDigit(digit) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraCardElevated,
                            contentColor = AuroraTextPrimary
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(width = 96.dp, height = 56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = digit,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            )
                            if (letters.isNotEmpty()) {
                                Text(
                                    text = letters,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuroraTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
        if (dtmfError != null) {
            Text(
                text = dtmfError,
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraAmber,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Digits are sent as you press them — use this for menus (“press 1”), extensions, and PINs.",
                style = MaterialTheme.typography.bodySmall,
                color = AuroraTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Call clock, m:ss — the original app's callClock format. */
private fun formatCallClock(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
