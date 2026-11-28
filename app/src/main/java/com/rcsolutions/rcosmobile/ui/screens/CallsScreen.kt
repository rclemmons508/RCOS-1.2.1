package com.rcsolutions.rcosmobile.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PhoneMissed
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rcsolutions.rcosmobile.data.LiveCall
import com.rcsolutions.rcosmobile.data.OutboundStatus
import com.rcsolutions.rcosmobile.data.ServerPhoneCall
import com.rcsolutions.rcosmobile.data.parseIso8601ToTimestamp
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallDirection
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.CallStatus
import com.rcsolutions.rcosmobile.model.LiveVoiceStatus
import com.rcsolutions.rcosmobile.model.LiveVoiceUiState
import com.rcsolutions.rcosmobile.model.OutboundCallStatus
import com.rcsolutions.rcosmobile.model.OutboundCallUiState
import com.rcsolutions.rcosmobile.model.VoicemailRecord
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSectionHeader
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One server-log entry mapped onto the app's call record shape. The
 * server's log is the ONLY record of receptionist-answered calls (it
 * writes them to its own store, not Firestore), so the Calls screen
 * merges it with the Firestore log. The two sources are disjoint —
 * the app's dialer logs outbound calls to Firestore only, never to
 * the server — so no dedupe is needed. `createdAt` is parsed from
 * the server's ISO timestamp so the merged list sorts newest first. */
private fun ServerPhoneCall.toCallRecord(): CallRecord {
    val ts = parseIso8601ToTimestamp(timestamp)
    val timeLabel = ts?.let { SimpleDateFormat("h:mm a", Locale.US).format(it.toDate()) } ?: ""
    val durationLabel = when {
        duration >= 60 -> "${duration / 60}m ${duration % 60}s"
        duration > 0 -> "${duration}s"
        else -> "—"
    }
    return CallRecord(
        id = "server_$id",
        callerName = callerName.ifBlank { callerNumber },
        callerNumber = callerNumber,
        direction = if (type == "outbound") CallDirection.OUTBOUND.code else CallDirection.INBOUND.code,
        status = when (status) {
            "missed" -> CallStatus.MISSED.code
            "voicemail" -> CallStatus.VOICEMAIL.code
            else -> CallStatus.COMPLETED.code
        },
        durationLabel = durationLabel,
        timeLabel = timeLabel,
        summary = summary,
        answeredBy = answeredBy.ifBlank { "ai_receptionist" },
        createdAt = ts,
    )
}

/** Merge the Firestore log with the server's log, newest first. */
private fun mergeCallLogs(firestoreCalls: List<CallRecord>, serverCalls: List<ServerPhoneCall>): List<CallRecord> {
    val merged = firestoreCalls + serverCalls.map { it.toCallRecord() }
    return merged.sortedByDescending { it.createdAt?.seconds ?: 0L }
}

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
    liveVoiceState: LiveVoiceUiState,
    // 8 fixes Nov 26 (item 1) — live calls the receptionist is
    // handling right now (GET /api/calls/live) and the server's
    // finished-call log (GET /api/phone/calls). Errors are passed
    // through so the screen can say so honestly instead of showing a
    // fake empty log.
    liveCalls: List<LiveCall> = emptyList(),
    liveCallsError: String? = null,
    serverCalls: List<ServerPhoneCall> = emptyList(),
    serverCallsError: String? = null,
    onStartLivePolling: () -> Unit = {},
    onStopLivePolling: () -> Unit = {},
    onToggleAiReceptionist: (Boolean) -> Unit,
    onStartLiveReceptionistCall: () -> Unit,
    onStopLiveReceptionistCall: () -> Unit,
    onLogCall: (callerName: String, phone: String, direction: CallDirection, status: CallStatus, duration: String, summary: String) -> Unit,
    outboundCallState: OutboundCallUiState = OutboundCallUiState(),
    outboundStatus: OutboundStatus? = null,
    onRefreshOutboundStatus: () -> Unit = {},
    onStartOutboundCall: (name: String, phone: String, note: String) -> Unit = { _, _, _ -> },
    onDismissOutboundError: () -> Unit = {},
    onMarkVoicemailReviewed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    var showMakeCallDialog by remember { mutableStateOf(false) }
    var micDeniedMessage by remember { mutableStateOf<String?>(null) }
    // Dialpad state. The call button opens the Dial & Log dialog
    // prefilled with the dialed number — confirming it starts the
    // in-app LiveKit call.
    var dialedNumber by remember { mutableStateOf("") }
    var dialpadPrefill by remember { mutableStateOf("") }
    // Ryan (Nov 27) — the keypad ate the bottom half of the Calls
    // screen at all times, squeezing the log. Collapsed by default;
    // one tap on Dialpad opens it inline, the same affordance hides
    // it again. Per-screen state only (remember); leaving the screen
    // resets it to hidden.
    var showDialpad by remember { mutableStateOf(false) }

    // 8 fixes Nov 26 (item 1) — poll the server for live calls and the
    // finished-call log while this screen is visible; stop when it
    // leaves, so no polling runs in the background.
    DisposableEffect(Unit) {
        onStartLivePolling()
        onDispose { onStopLivePolling() }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micDeniedMessage = null
            onStartLiveReceptionistCall()
        } else {
            micDeniedMessage = "Microphone permission is required to talk live with the AI Receptionist."
        }
    }

    // Outbound dialer: the Dial & Log dialog's confirm starts an in-app
    // LiveKit call (not the phone dialer). The mic permission result
    // arrives after the dialog's values are captured, so they wait here.
    var pendingDialName by remember { mutableStateOf("") }
    var pendingDialPhone by remember { mutableStateOf("") }
    var pendingDialNote by remember { mutableStateOf("") }
    val dialMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartOutboundCall(pendingDialName, pendingDialPhone, pendingDialNote)
            dialedNumber = ""
            dialpadPrefill = ""
            showMakeCallDialog = false
        }
        // Denied: the dialog stays open and the user can retry. The app
        // never pretends a call started.
    }

    // Outbound trunk status, refreshed whenever the business changes.
    LaunchedEffect(business?.id) {
        onRefreshOutboundStatus()
    }

    // Audit Phase 1 — hide outbound dialing entirely when the server
    // reports the trunk unconfigured, instead of showing the Vonage
    // setup error to end users. Unknown status (null — never fetched
    // or the fetch failed) fails OPEN: the dialer stays visible
    // until the server positively says it is not configured. (Ryan's
    // trunk IS configured, so the dialer stays visible for him.)
    // The call-back rows below are NOT gated: they open the phone's
    // own dialer (ACTION_DIAL), which does not use the trunk.
    val outboundDialingAvailable = outboundStatus?.configured != false

    val aiEnabled = business?.aiReceptionistEnabled ?: true

    // 8 fixes Nov 26 (items 1d + 2) — the log is the Firestore log
    // MERGED with the server's log, so receptionist-answered calls
    // finally appear here. Newest first. The counts below follow the
    // merged list.
    val mergedCalls = remember(calls, serverCalls) { mergeCallLogs(calls, serverCalls) }
    val nowSeconds = remember { System.currentTimeMillis() / 1000L }
    val startOfDaySec = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis / 1000L
    }
    val todayCount = mergedCalls.count { (it.createdAt?.seconds ?: nowSeconds) >= startOfDaySec }
    val missedCount = mergedCalls.count { it.status == CallStatus.MISSED.code || it.direction == CallDirection.MISSED.code }
    val weekCount = mergedCalls.count { (it.createdAt?.seconds ?: nowSeconds) >= nowSeconds - 7L * 86400L }

    val filteredCalls = remember(mergedCalls, selectedFilter) {
        when (selectedFilter) {
            CallFilter.ALL -> mergedCalls
            CallFilter.ACTIVE -> mergedCalls.filter { it.status == CallStatus.ACTIVE.code || it.answeredBy == "ai_receptionist" }
            CallFilter.MISSED -> mergedCalls.filter { it.status == CallStatus.MISSED.code || it.direction == CallDirection.MISSED.code }
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
            // 8 fixes Nov 26 (item 1c) — a call the receptionist is
            // handling RIGHT NOW. Polled every 6s while this screen is
            // visible. Never faked: the card only appears when the
            // server actually reports a live call.
            liveCalls.forEach { liveCall ->
                item(key = "live_${liveCall.roomName}") {
                    AuroraCard(
                        highlighted = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AuroraEmerald.copy(alpha = 0.16f))
                                    .border(1.dp, AuroraEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneInTalk,
                                    contentDescription = "Live call",
                                    tint = AuroraEmerald
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live now — ${liveCall.callerNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AuroraTextPrimary
                                )
                                Text(
                                    text = if (liveCall.status == "answered") "Receptionist is talking to this caller" else "Ringing — receptionist is picking up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraEmerald
                                )
                            }
                        }
                    }
                }
            }
            if (liveCalls.isEmpty() && liveCallsError != null) {
                item {
                    Text(
                        text = "Live calls unavailable — couldn't reach the server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraAmber,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
                    Spacer(Modifier.height(10.dp))
                    val isLive = liveVoiceState.status == LiveVoiceStatus.LIVE ||
                        liveVoiceState.status == LiveVoiceStatus.CONNECTING
                    OutlinedButton(
                        onClick = {
                            if (isLive) {
                                onStopLiveReceptionistCall()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    micDeniedMessage = null
                                    onStartLiveReceptionistCall()
                                } else {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isLive) AuroraRed else AuroraEmerald),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isLive) AuroraRed.copy(alpha = 0.14f) else AuroraEmerald.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("calls_live_ai_voice_button")
                    ) {
                        Icon(
                            imageVector = if (isLive) Icons.Outlined.CallEnd else Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = if (isLive) AuroraRed else AuroraEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (liveVoiceState.status) {
                                LiveVoiceStatus.CONNECTING -> "Connecting to Gemini Live (${liveVoiceState.modelId})..."
                                LiveVoiceStatus.LIVE -> "End Live AI Voice (${liveVoiceState.durationSeconds}s)"
                                else -> "Talk Live with AI Receptionist"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLive) AuroraRed else AuroraEmerald
                        )
                    }
                    val errText = micDeniedMessage ?: liveVoiceState.error
                    if (!errText.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = errText,
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraRed
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

            // 8 fixes Nov 26 (item 2) — the call log, visible on this
            // screen without opening a dialog. The totals live in the
            // metrics card above; the old "Full Call & Receptionist
            // Log" dialog is gone.
            item {
                Column {
                    AuroraSectionHeader(title = "Call Log")
                    Text(
                        text = "AI Receptionist: ${if (aiEnabled) "Active (24/7)" else "Manual Ring"} • ${mergedCalls.size} calls • ${voicemails.size} voicemails",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraTextSecondary
                    )
                    if (serverCallsError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Server call log unavailable — showing calls saved on this device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraAmber
                        )
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
                        // Audit Phase 1 — no dial entry when the trunk
                        // is unconfigured (see outboundDialingAvailable
                        // above); the empty state itself stays.
                        actionLabel = if (outboundDialingAvailable) "+ Make a Call" else null,
                        onActionClick = if (outboundDialingAvailable) {
                            { dialpadPrefill = ""; showMakeCallDialog = true }
                        } else null
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
                                // 8 fixes Nov 26 (item 2) — the
                                // receptionist's summary, when the
                                // server recorded one, so the row says
                                // what the call was about.
                                if (call.summary.isNotBlank()) {
                                    Text(
                                        text = call.summary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AuroraTextMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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

        // Dialpad — numeric keys wired into the Dial & Log flow.
        // Dialing a number and tapping the call button opens the Dial &
        // Log dialog prefilled with it; name and note stay editable
        // there before the in-app call starts. The call is logged as
        // outbound when it ends. Hidden entirely (Audit Phase 1) when
        // the server reports the trunk unconfigured — no dialpad, no
        // setup error shown to end users. Collapsed by default (Ryan,
        // Nov 27): the keypad ate the bottom half of the screen. The
        // toggle below opens it inline; the same affordance hides it
        // again. The "Make a Call" CTA never depended on the keypad.
        if (outboundDialingAvailable) {
            AuroraSecondaryCta(
                text = if (showDialpad) "Hide dialpad" else "Dialpad",
                icon = Icons.Outlined.Dialpad,
                testTag = "calls_dialpad_toggle",
                onClick = { showDialpad = !showDialpad }
            )
            if (showDialpad) {
                Spacer(Modifier.height(10.dp))
                DialpadCard(
                    dialedNumber = dialedNumber,
                    onKeyPress = { key ->
                        if (dialedNumber.length < 15) dialedNumber += key
                    },
                    onBackspace = {
                        if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                    },
                    onCall = {
                        dialpadPrefill = dialedNumber
                        showMakeCallDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (outboundCallState.status == OutboundCallStatus.ERROR) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = outboundCallState.error ?: "The call could not be started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraRed,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissOutboundError) {
                    Text("Dismiss", color = AuroraTextSecondary)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Bottom Dual CTAs matching Screen 3. Hidden (Audit Phase 1)
        // when the trunk is unconfigured — same gate as the dialpad.
        if (outboundDialingAvailable) {
            AuroraPrimaryCta(
                text = "Make a Call",
                icon = Icons.Outlined.Add,
                testTag = "calls_make_call_button",
                onClick = { dialpadPrefill = ""; showMakeCallDialog = true }
            )
        }
    }

    if (showMakeCallDialog) {
        var nameInput by remember { mutableStateOf("") }
        // Prefilled with the dialpad number when the call started
        // there; blank when opened from Make a Call.
        var phoneInput by remember { mutableStateOf(dialpadPrefill) }
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
                        // In-app LiveKit call now — the phone dialer
                        // (ACTION_DIAL) is gone from this path. The call
                        // is logged as outbound when it ends (only when
                        // it actually connected); nothing is logged for
                        // a call that never connected.
                        pendingDialName = safeName
                        pendingDialPhone = safePhone
                        pendingDialNote = notesInput.trim()
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            onStartOutboundCall(safeName, safePhone, notesInput.trim())
                            dialedNumber = ""
                            dialpadPrefill = ""
                            showMakeCallDialog = false
                        } else {
                            dialMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
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

/** Numeric dialpad (0-9, *, #, backspace, call). The call button
 * opens the Dial & Log dialog prefilled with the dialed number —
 * confirming it starts an in-app LiveKit call (not the phone dialer)
 * and logs it as outbound when it ends. */
@Composable
private fun DialpadCard(
    dialedNumber: String,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuroraCard(modifier = modifier) {
        Text(
            text = dialedNumber.ifBlank { "Enter a number" },
            style = MaterialTheme.typography.headlineSmall,
            color = if (dialedNumber.isBlank()) AuroraTextMuted else AuroraTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().testTag("dialpad_number_display")
        )
        Spacer(Modifier.height(10.dp))
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    DialpadKey(label = key, onClick = { onKeyPress(key) })
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackspace,
                enabled = dialedNumber.isNotEmpty(),
                modifier = Modifier.size(56.dp).testTag("dialpad_backspace")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backspace,
                    contentDescription = "Delete last digit",
                    tint = if (dialedNumber.isNotEmpty()) AuroraTextSecondary else AuroraTextMuted
                )
            }
            Button(
                onClick = onCall,
                enabled = dialedNumber.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraEmerald,
                    contentColor = AuroraOnEmerald,
                    disabledContainerColor = AuroraEmerald.copy(alpha = 0.35f)
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(64.dp).testTag("dialpad_call_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(28.dp)
                )
            }
            // Balances the backspace button so the call button stays centered.
            Spacer(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun DialpadKey(
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, AuroraBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AuroraCardElevated,
            contentColor = AuroraTextPrimary
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(64.dp).testTag("dialpad_key_$label")
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
