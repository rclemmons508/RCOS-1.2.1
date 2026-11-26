package com.rcsolutions.rcosmobile.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rcsolutions.rcosmobile.data.LocalPrefsState
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.AIEmployee
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.AgentSettings
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.AutonomyMode
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.TeamMessage
import com.rcsolutions.rcosmobile.notifications.appNotificationSettingsIntent
import com.rcsolutions.rcosmobile.notifications.areNotificationsEnabled
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraDivider
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

private enum class AccountModal {
    NONE,
    BUSINESS_PROFILE,
    SUBSCRIPTION_AUTONOMY,
    AI_TEAM_RULES,
    NOTIFICATIONS,
    INTEGRATIONS_TEAM_CHAT,
    SECURITY,
    HELP_REPORTS
}

@Composable
fun AccountScreen(
    ownerName: String,
    ownerEmail: String,
    currentUserId: String,
    business: BusinessAccount?,
    allBusinesses: List<BusinessAccount>,
    localPrefs: LocalPrefsState,
    teamMessages: List<TeamMessage>,
    auditLogs: List<AuditLog>,
    actions: List<ActionRecord>,
    onSwitchBusiness: (String) -> Unit,
    onCreateNewWorkspace: () -> Unit,
    onUpdateBusiness: (Map<String, Any>, String) -> Unit,
    onToggleEmployee: (String, Boolean) -> Unit,
    onSaveAgentSettings: (String, AgentSettings) -> Unit,
    onToggleAutomationMaster: (Boolean) -> Unit,
    onToggleAutomationRule: (String, Boolean) -> Unit,
    onSaveNotificationPrefs: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onSendTeamMessage: (String) -> Unit,
    onDeleteWorkspace: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeModal by remember { mutableStateOf(AccountModal.NONE) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Agent being edited (null = no edit screen open).
    var editingEmployee by remember { mutableStateOf<AIEmployee?>(null) }

    // Phone notification permission (item 7). The launcher is
    // registered here, at the top level, so it survives the modal
    // opening and closing.
    val context = LocalContext.current
    var phoneNotifGranted by remember { mutableStateOf(areNotificationsEnabled(context)) }
    var notifRequestDenied by remember { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        phoneNotifGranted = granted
        if (!granted) notifRequestDenied = true
    }

    val initials = remember(ownerName, ownerEmail) {
        val base = ownerName.ifBlank { ownerEmail.substringBefore("@") }
        val parts = base.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size >= 2) "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        else base.take(2).uppercase().ifBlank { "RC" }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Account", style = MaterialTheme.typography.headlineMedium, color = AuroraTextPrimary)
            Text("Manage your profile and preferences.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
        }

        // Owner Profile Card matching Screen 7
        item {
            AuroraCard(
                highlighted = true,
                onClick = { activeModal = AccountModal.BUSINESS_PROFILE },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AuroraEmerald.copy(alpha = 0.2f))
                            .border(1.5.dp, AuroraEmerald, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuroraEmerald
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ownerName.ifBlank { ownerEmail },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuroraTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Owner • ${business?.name ?: "No Workspace"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AuroraTextMuted)
                }
            }
        }

        // Settings Menu Rows matching Screen 7
        item {
            AuroraCard(contentPadding = PaddingValues(0.dp), modifier = Modifier.fillMaxWidth()) {
                AccountMenuRow(
                    icon = Icons.Outlined.Business,
                    title = "Business Profile",
                    subtitle = business?.industry?.let { RcosDefaults.findPreset(it).name },
                    testTag = "account_business_profile",
                    onClick = { activeModal = AccountModal.BUSINESS_PROFILE }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.CreditCard,
                    title = "Subscription & Autonomy",
                    badgeText = business?.subscriptionTier ?: "Pro Plan",
                    testTag = "account_subscription",
                    onClick = { activeModal = AccountModal.SUBSCRIPTION_AUTONOMY }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "AI Team & Automation",
                    subtitle = "${business?.activeEmployees?.count { it.value } ?: 12} active specialists",
                    testTag = "account_ai_team",
                    onClick = { activeModal = AccountModal.AI_TEAM_RULES }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    testTag = "account_notifications",
                    onClick = { activeModal = AccountModal.NOTIFICATIONS }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.Hub,
                    title = "Integrations & Team Chat",
                    testTag = "account_integrations",
                    onClick = { activeModal = AccountModal.INTEGRATIONS_TEAM_CHAT }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = "Google Sign-In & Workspace Isolation",
                    testTag = "account_security",
                    onClick = { activeModal = AccountModal.SECURITY }
                )
                AuroraDivider()
                AccountMenuRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help, Reports & Audit Log",
                    testTag = "account_help",
                    onClick = { activeModal = AccountModal.HELP_REPORTS }
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("account_logout_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AuroraBorder)
            ) {
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AuroraTextPrimary
                )
            }
        }
    }

    // ---- Modal Dialogs for Each Settings Section ----
    when (activeModal) {
        AccountModal.BUSINESS_PROFILE -> {
            var bizName by remember(business?.id) { mutableStateOf(business?.name.orEmpty()) }
            var area by remember(business?.id) { mutableStateOf(business?.serviceArea.orEmpty()) }
            var hours by remember(business?.id) { mutableStateOf(business?.businessHours.orEmpty()) }
            var knowledge by remember(business?.id) { mutableStateOf(business?.businessKnowledge.orEmpty()) }

            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Business Profile", color = AuroraTextPrimary) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Workspaces: every business the owner owns or is
                        // a member of. Same-named workspaces are
                        // distinguished by their real trade and created
                        // date — never hidden, never deleted from here.
                        // The three data-bearing duplicates stay listed
                        // until the owner decides whether to move that
                        // data into the live business. Single-workspace
                        // case: one row, "Current Workspace", no empty
                        // list — and multi-workspace keeps working if a
                        // test workspace is added later.
                        val workspaces = remember(allBusinesses) { allBusinesses.distinctBy { it.id } }
                        Text(
                            text = if (workspaces.size > 1) "Switch Workspace (${workspaces.size}):" else "Current Workspace:",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuroraEmerald
                        )
                        if (workspaces.isEmpty()) {
                            Text(
                                "No workspaces yet — create one below to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuroraTextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                workspaces.forEach { b ->
                                    val isActive = b.id == business?.id
                                    val subtitle = workspaceSubtitle(b)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onSwitchBusiness(b.id); activeModal = AccountModal.NONE }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.name,
                                                color = if (isActive) AuroraEmerald else AuroraTextPrimary,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (subtitle.isNotBlank()) {
                                                Text(subtitle, color = AuroraTextMuted, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        if (isActive) {
                                            Text("✓", color = AuroraEmerald, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                        AuroraDivider()
                        OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Company Name") }, singleLine = true, colors = auroraFieldColors())
                        OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Service Area / City") }, singleLine = true, colors = auroraFieldColors())
                        OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Operating Hours") }, singleLine = true, colors = auroraFieldColors())
                        OutlinedTextField(value = knowledge, onValueChange = { knowledge = it }, label = { Text("Teach Your Team (Pricing & Policies)") }, minLines = 3, maxLines = 5, colors = auroraFieldColors())
                        TextButton(onClick = { activeModal = AccountModal.NONE; onCreateNewWorkspace() }) {
                            Text("+ Create Another Business Workspace", color = AuroraEmerald)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (bizName.isNotBlank()) {
                            onUpdateBusiness(
                                mapOf(
                                    "name" to bizName.trim().take(100),
                                    "serviceArea" to area.trim().take(120),
                                    "businessHours" to hours.trim().take(120),
                                    "businessKnowledge" to knowledge.trim().take(8000)
                                ),
                                "Business profile updated."
                            )
                        }
                        activeModal = AccountModal.NONE
                    }) { Text("Save", color = AuroraEmerald) }
                },
                dismissButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Cancel", color = AuroraTextSecondary) }
                }
            )
        }

        AccountModal.SUBSCRIPTION_AUTONOMY -> {
            var mode by remember(business?.id) { mutableStateOf(AutonomyMode.fromCode(business?.autonomyMode)) }
            var limit by remember(business?.id) { mutableDoubleStateOf(business?.dollarThreshold ?: 250.0) }
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Plan & Approval Rules", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Active Tier: ${business?.subscriptionTier ?: "Pro Plan"}", color = AuroraEmerald, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Supervised Approval Mode", color = AuroraTextPrimary)
                            Switch(
                                checked = mode == AutonomyMode.SUPERVISED,
                                onCheckedChange = { mode = if (it) AutonomyMode.SUPERVISED else AutonomyMode.AUTONOMOUS },
                                colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
                            )
                        }
                        Text("Threshold: $${limit.toInt()}", color = AuroraTextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100.0, 250.0, 500.0, 1000.0).forEach { amt ->
                                TextButton(onClick = { limit = amt }) {
                                    Text("$${amt.toInt()}", color = if (limit == amt) AuroraEmerald else AuroraTextMuted)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onUpdateBusiness(
                            mapOf("autonomyMode" to mode.code, "dollarThreshold" to limit),
                            "Safety rules saved."
                        )
                        activeModal = AccountModal.NONE
                    }) { Text("Save", color = AuroraEmerald) }
                },
                dismissButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Close", color = AuroraTextSecondary) }
                }
            )
        }

        AccountModal.AI_TEAM_RULES -> {
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("AI Team & Automation", color = AuroraTextPrimary) },
                text = {
                    // Scrollable: the old fixed column overflowed with
                    // 13+ rows, so the bottom toggles (Support Assistant)
                    // rendered under the button bar, doubled/overlapping.
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Automation Engine", color = AuroraEmerald, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = localPrefs.automationMasterEnabled,
                                onCheckedChange = onToggleAutomationMaster,
                                colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
                            )
                        }
                        RcosDefaults.DEFAULT_AUTOMATION_RULES.forEach { rule ->
                            val enabled = rule.id !in localPrefs.disabledAutomationRuleIds
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(rule.name, color = AuroraTextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onToggleAutomationRule(rule.id, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
                                )
                            }
                        }
                        AuroraDivider()
                        Text("Active AI Specialists:", color = AuroraEmerald, style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Tap an agent to edit its info, permissions, and thresholds.",
                            color = AuroraTextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        RcosDefaults.AI_EMPLOYEES.take(6).forEach { emp ->
                            val isOn = business?.activeEmployees?.get(emp.id) ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { editingEmployee = emp }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.name, color = AuroraTextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text(emp.tagline, color = AuroraTextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Switch(
                                    checked = isOn,
                                    onCheckedChange = { onToggleEmployee(emp.id, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
                                )
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AuroraTextMuted)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Done", color = AuroraEmerald) }
                }
            )
        }

        AccountModal.NOTIFICATIONS -> {
            var push by remember { mutableStateOf(localPrefs.pushAlertsEnabled) }
            var sound by remember { mutableStateOf(localPrefs.soundEnabled) }
            var vib by remember { mutableStateOf(localPrefs.vibrationEnabled) }
            var quiet by remember { mutableStateOf(localPrefs.quietHoursEnabled) }
            // Re-read the system grant each time this opens — the owner
            // may have flipped the toggle in the phone's Settings.
            LaunchedEffect(Unit) {
                phoneNotifGranted = areNotificationsEnabled(context)
            }
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Notifications", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Phone notifications (item 7): the system
                        // toggle in Settings would not stay on because
                        // the app never declared the permission or
                        // created a channel. Both now exist; this row
                        // shows the real grant state and offers the
                        // runtime request (Android 13+). If the request
                        // was denied, the button opens the app's
                        // notification settings instead.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Phone Notifications", style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
                                Text(
                                    text = if (phoneNotifGranted) "On — call alerts can reach your lock screen."
                                    else "Off — turn on to receive call alerts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextMuted
                                )
                            }
                            if (!phoneNotifGranted) {
                                TextButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifRequestDenied) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(appNotificationSettingsIntent(context))
                                    }
                                }) {
                                    Text(
                                        if (notifRequestDenied) "Open Settings" else "Enable",
                                        color = AuroraEmerald
                                    )
                                }
                            }
                        }
                        AuroraDivider()
                        SettingSwitchRow("In-App Operational Alerts", push) { push = it }
                        SettingSwitchRow("Alert Sounds", sound) { sound = it }
                        SettingSwitchRow("Haptic Vibration", vib) { vib = it }
                        SettingSwitchRow("Quiet Hours (10 PM – 7 AM)", quiet) { quiet = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onSaveNotificationPrefs(push, sound, vib, quiet)
                        activeModal = AccountModal.NONE
                    }) { Text("Save", color = AuroraEmerald) }
                },
                dismissButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Cancel", color = AuroraTextSecondary) }
                }
            )
        }

        AccountModal.INTEGRATIONS_TEAM_CHAT -> {
            // Real chat thread (full-screen dialog): bubbles, timestamps,
            // input + send, real empty state. The old "Connected
            // Backends" text block and the bare "Message team..." box
            // are gone. Still wired to the same teamMessages source.
            Dialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AuroraCardElevated
                ) {
                    TeamChatThread(
                        messages = teamMessages,
                        currentUserId = currentUserId,
                        onSend = onSendTeamMessage,
                        onClose = { activeModal = AccountModal.NONE }
                    )
                }
            }
        }

        AccountModal.SECURITY -> {
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Security & Privacy", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Authenticated via Google Sign-In. Workspace data is scoped strictly to your business account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextSecondary
                        )
                        AuroraDivider()
                        Button(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraRed.copy(alpha = 0.2f), contentColor = AuroraRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Workspace Data")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Close", color = AuroraEmerald) }
                }
            )
        }

        AccountModal.HELP_REPORTS -> {
            val totalApproved = actions.filter { it.status == "approved" || it.status == "completed" }.sumOf { it.dollarAmount }
            val paidRevenue = actions.filter { it.paymentStatus == "paid" }.sumOf { it.dollarAmount }
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Reports & Audit Trail", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Approved Volume: $${String.format(Locale.US, "%,.2f", totalApproved)}\nPaid Invoices: $${String.format(Locale.US, "%,.2f", paidRevenue)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = AuroraEmerald
                        )
                        AuroraDivider()
                        Text("Recent Audit Trail (${auditLogs.size}):", style = MaterialTheme.typography.labelMedium, color = AuroraTextSecondary)
                        auditLogs.take(5).forEach { log ->
                            Text("• ${log.summary}", style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
                        }
                        if (auditLogs.isEmpty()) {
                            Text("No approval decisions logged yet.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { activeModal = AccountModal.NONE }) { Text("Done", color = AuroraEmerald) }
                }
            )
        }

        AccountModal.NONE -> Unit
    }

    // Agent edit screen (full-screen overlay over Account). Opens when
    // an agent row is tapped in AI Team & Automation.
    val employeeToEdit = editingEmployee
    if (employeeToEdit != null) {
        Dialog(
            onDismissRequest = { editingEmployee = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AuroraCardElevated
            ) {
                AgentEditScreen(
                    business = business,
                    employee = employeeToEdit,
                    onSave = { settings -> onSaveAgentSettings(employeeToEdit.id, settings) },
                    onClose = { editingEmployee = null }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AuroraCardElevated,
            title = { Text("Permanently Delete Workspace?", color = AuroraRed) },
            text = { Text("This deletes all jobs, customers, tasks, quotes, calls, and logs for ${business?.name ?: "this workspace"}.", color = AuroraTextPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    activeModal = AccountModal.NONE
                    onDeleteWorkspace()
                }) { Text("Delete Forever", color = AuroraRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = AuroraTextSecondary) }
            }
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AuroraEmerald, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AuroraTextMuted)
                }
            }
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AuroraEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AuroraEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AuroraTextMuted)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
        )
    }
}

/** Distinguishes same-named workspaces with real data only: the
 * trade (industry preset) and the date the workspace was created.
 * Example: "Landscaping · Created Nov 17, 2026". Parts with no data
 * are left out — nothing is invented. */
private fun workspaceSubtitle(business: BusinessAccount): String {
    val trade = RcosDefaults.findPreset(business.industry).name
    val created = business.createdAt?.toDate()?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(it)
    }
    return listOfNotNull(
        trade.takeIf { it.isNotBlank() },
        created?.let { "Created $it" }
    ).joinToString(" · ")
}

/** Team chat as a real message thread: bubbles (own vs others),
 * timestamps, input + send, and a proper empty state. */
@Composable
private fun TeamChatThread(
    messages: List<TeamMessage>,
    currentUserId: String,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val sorted = remember(messages) {
        messages.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(sorted.size) {
        if (sorted.isNotEmpty()) listState.animateScrollToItem(sorted.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) {
                Text("‹ Back", color = AuroraTextSecondary)
            }
            Text(
                text = "Team Chat (${sorted.size})",
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary
            )
            TextButton(onClick = onClose) {
                Text("Done", color = AuroraEmerald)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No team messages yet", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Say hello — messages here go to everyone on your team.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.id.ifBlank { "${it.senderId}-${it.createdAt?.seconds ?: 0}" } }) { message ->
                    val isOwn = message.senderId == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isOwn) AuroraEmerald.copy(alpha = 0.18f) else AuroraCardElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isOwn) AuroraEmerald.copy(alpha = 0.4f) else AuroraBorder
                            ),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                if (!isOwn) {
                                    Text(
                                        message.senderName.ifBlank { "Teammate" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AuroraEmerald
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(message.text, style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatChatTime(message.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message team...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("team_chat_input"),
                colors = auroraFieldColors()
            )
            Button(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft.trim())
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraEmerald,
                    contentColor = AuroraOnEmerald,
                    disabledContainerColor = AuroraEmerald.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("team_chat_send_button")
            ) {
                Text("Send")
            }
        }
    }
}

private fun formatChatTime(createdAt: com.google.firebase.Timestamp?): String {
    val date = createdAt?.toDate() ?: return ""
    return SimpleDateFormat("h:mm a", Locale.US).format(date)
}
