package com.rcsolutions.rcosmobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.PhoneMissed
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallDirection
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.CallStatus
import com.rcsolutions.rcosmobile.model.VoicemailRecord
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraDivider
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.util.Calendar

private enum class CallFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    MISSED("Missed"),
    VOICEMAIL("Voicemail")
}

@Composable
fun CallsScreen(
    business: BusinessAccount?,
    calls: List<CallRecord>,
    voicemails: List<VoicemailRecord>,
    onToggleAiReceptionist: (Boolean) -> Unit,
    onLogCall: (callerName: String, phone: String, direction: CallDirection, status: CallStatus, duration: String, summary: String) -> Unit,
    onMarkVoicemailReviewed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    var showMakeCallDialog by remember { mutableStateOf(false) }
    var showFullLogDialog by remember { mutableStateOf(false) }

    val aiEnabled = business?.aiReceptionistEnabled ?: true

    val nowSeconds = remember { System.currentTimeMillis() / 1000L }
    val startOfDaySec = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis / 1000L
    }
    val todayCount = calls.count { (it.createdAt?.seconds ?: nowSeconds) >= startOfDaySec }
    val missedCount = calls.count { it.status == CallStatus.MISSED.code || it.direction == CallDirection.MISSED.code }
    val weekCount = calls.count { (it.createdAt?.seconds ?: nowSeconds) >= nowSeconds - 7L * 86400L }

    val filteredCalls = remember(calls, selectedFilter) {
        when (selectedFilter) {
            CallFilter.ALL -> calls
            CallFilter.ACTIVE -> calls.filter { it.status == CallStatus.ACTIVE.code || it.answeredBy == "ai_receptionist" }
            CallFilter.MISSED -> calls.filter { it.status == CallStatus.MISSED.code || it.direction == CallDirection.MISSED.code }
            CallFilter.VOICEMAIL -> emptyList()
        }
    }

    fun launchDialer(phoneNumber: String) {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        runCatching { context.startActivity(intent) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Calls",
            style = MaterialTheme.typography.headlineMedium,
            color = AuroraTextPrimary
        )
        Text(
            text = "Manage all your communications.",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
        Spacer(Modifier.height(12.dp))

        // 4 Filter Pills matching Screen 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CallFilter.entries.forEach { filter ->
                AuroraFilterPill(
                    label = filter.label,
                    selected = selectedFilter == filter,
                    testTag = "calls_filter_${filter.name.lowercase()}",
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // AI Receptionist Live Toggle Card
            item {
                AuroraCard(
                    highlighted = aiEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuroraEmerald.copy(alpha = 0.16f))
                                .border(1.dp, AuroraEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HeadsetMic,
                                contentDescription = "AI Receptionist",
                                tint = AuroraEmerald
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Receptionist",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuroraTextPrimary
                            )
                            Text(
                                text = if (aiEnabled) "Handling calls 24/7 • Live and ready" else "Paused • Manual ring mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aiEnabled) AuroraEmerald else AuroraTextMuted
                            )
                        }
                        Switch(
                            checked = aiEnabled,
                            onCheckedChange = onToggleAiReceptionist,
                            modifier = Modifier.testTag("calls_ai_receptionist_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AuroraOnEmerald,
                                checkedTrackColor = AuroraEmerald
                            )
                        )
                    }
                }
            }

            // 3-Column Metrics Card (Today | Missed | This Week)
            item {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallStatCell(todayCount.toString(), "Today", Modifier.weight(1f))
                        CallStatCell(missedCount.toString(), "Missed", Modifier.weight(1f), isAlert = missedCount > 0)
                        CallStatCell(weekCount.toString(), "This Week", Modifier.weight(1f))
                    }
                }
            }

            if (selectedFilter == CallFilter.VOICEMAIL) {
                if (voicemails.isEmpty()) {
                    item {
                        AuroraEmptyState(
                            icon = Icons.Outlined.Voicemail,
                            title = "No voicemails right now",
                            subtitle = "New caller voicemails and AI transcriptions appear here."
                        )
                    }
                } else {
                    items(voicemails, key = { it.id }) { vm ->
                        AuroraCard(
                            highlighted = !vm.reviewed,
                            onClick = { onMarkVoicemailReviewed(vm.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Voicemail, contentDescription = null, tint = AuroraEmerald)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(vm.callerName, style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                                    Text("${vm.callerNumber} • ${vm.durationLabel}", style = MaterialTheme.typography.labelSmall, color = AuroraTextSecondary)
                                }
                                Text(
                                    text = if (vm.reviewed) "Reviewed" else "Unread",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (vm.reviewed) AuroraTextMuted else AuroraEmerald
                                )
                            }
                            if (vm.transcription.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = vm.transcription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextSecondary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { launchDialer(vm.callerNumber) }) {
                                Text("Call Back ${vm.callerNumber}", color = AuroraEmerald)
                            }
                        }
                    }
                }
            } else if (filteredCalls.isEmpty()) {
                item {
                    AuroraEmptyState(
                        icon = Icons.Outlined.Phone,
                        title = "No calls logged yet",
                        subtitle = "Tap + Make a Call below to dial a customer via your phone app and log the outcome.",
                        actionLabel = "+ Make a Call",
                        onActionClick = { showMakeCallDialog = true }
                    )
                }
            } else {
                items(filteredCalls, key = { it.id }) { call ->
                    val isMissed = call.status == CallStatus.MISSED.code || call.direction == CallDirection.MISSED.code
                    AuroraCard(
                        onClick = { launchDialer(call.callerNumber) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isMissed) AuroraRed.copy(alpha = 0.16f) else AuroraEmerald.copy(alpha = 0.14f))
                                    .border(1.dp, if (isMissed) AuroraRed.copy(alpha = 0.45f) else AuroraEmerald.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMissed) Icons.Outlined.PhoneMissed else Icons.Outlined.PhoneCallback,
                                    contentDescription = null,
                                    tint = if (isMissed) AuroraRed else AuroraEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = call.callerName.ifBlank { call.callerNumber },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AuroraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = if (isMissed) "Missed Call • ${call.callerNumber}"
                                else "${CallDirection.fromCode(call.direction).label} • ${call.durationLabel}"
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMissed) AuroraRed else AuroraTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = call.timeLabel.ifBlank { "Today" },
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Bottom Dual CTAs matching Screen 3
        AuroraPrimaryCta(
            text = "Make a Call",
            icon = Icons.Outlined.Add,
            testTag = "calls_make_call_button",
            onClick = { showMakeCallDialog = true }
        )
        Spacer(Modifier.height(8.dp))
        AuroraSecondaryCta(
            text = "View Full Call Log (${calls.size})",
            testTag = "calls_view_full_log_button",
            onClick = { showFullLogDialog = true }
        )
    }

    if (showMakeCallDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMakeCallDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Dial & Log Call", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Customer / Contact Name") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("(864) 555-0187") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Call Note (optional)") },
                        minLines = 2,
                        colors = auroraFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val safeName = nameInput.trim().ifBlank { phoneInput.trim() }
                    val safePhone = phoneInput.trim()
                    if (safePhone.isNotEmpty()) {
                        onLogCall(safeName, safePhone, CallDirection.OUTBOUND, CallStatus.COMPLETED, "2 min", notesInput)
                        launchDialer(safePhone)
                        showMakeCallDialog = false
                    }
                }) {
                    Text("Dial & Log", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMakeCallDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }

    if (showFullLogDialog) {
        AlertDialog(
            onDismissRequest = { showFullLogDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Full Call & Receptionist Log", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI Receptionist Status: ${if (aiEnabled) "Active (24/7)" else "Manual Ring"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = AuroraEmerald
                    )
                    Text(
                        text = "Total Logged Calls: ${calls.size}  •  Voicemails: ${voicemails.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                    AuroraDivider()
                    calls.take(6).forEach { c ->
                        Text(
                            text = "${c.callerName} (${c.callerNumber}) — ${c.status} (${c.timeLabel})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextPrimary
                        )
                    }
                    if (calls.isEmpty()) {
                        Text("No calls recorded yet.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullLogDialog = false }) {
                    Text("Done", color = AuroraEmerald)
                }
            }
        )
    }
}

@Composable
private fun CallStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) AuroraRed else AuroraTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AuroraTextSecondary
        )
    }
}
