package com.rcsolutions.rcosmobile.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rcsolutions.rcosmobile.data.LocalPrefsState
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.data.canAuthenticateBiometric
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.AutonomyMode
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.TeamMessage
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
    onToggleAutomationMaster: (Boolean) -> Unit,
    onToggleAutomationRule: (String, Boolean) -> Unit,
    onSaveNotificationPrefs: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onSendTeamMessage: (String) -> Unit,
    onSetBiometrics: (Boolean) -> Unit,
    onDeleteWorkspace: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeModal by remember { mutableStateOf(AccountModal.NONE) }
    var confirmDelete by remember { mutableStateOf(false) }

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
                            text = "Owner • ${business?.name ?: "RC Solutions"}",
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
                    subtitle = if (localPrefs.biometricsEnabled) "Biometric lock active" else "Standard Google Auth",
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (allBusinesses.size > 1) {
                            Text("Switch Workspace:", style = MaterialTheme.typography.labelMedium, color = AuroraEmerald)
                            allBusinesses.forEach { b ->
                                TextButton(onClick = { onSwitchBusiness(b.id); activeModal = AccountModal.NONE }) {
                                    Text(
                                        text = if (b.id == business?.id) "✓ ${b.name}" else b.name,
                                        color = if (b.id == business?.id) AuroraEmerald else AuroraTextPrimary
                                    )
                                }
                            }
                            AuroraDivider()
                        }
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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        RcosDefaults.AI_EMPLOYEES.take(6).forEach { emp ->
                            val isOn = business?.activeEmployees?.get(emp.id) ?: true
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(emp.name, color = AuroraTextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = isOn,
                                    onCheckedChange = { onToggleEmployee(emp.id, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AuroraOnEmerald, checkedTrackColor = AuroraEmerald)
                                )
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
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Notifications", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            var teamDraft by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Team Chat & Integrations", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connected Backends: Firebase Auth, Firestore Sync, Gemini 3.8 Flash AI Logic, Open-Meteo Weather, Android Dialer/SMS/Email Intents.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
                        AuroraDivider()
                        Text("Team Chat (${teamMessages.size})", style = MaterialTheme.typography.titleSmall, color = AuroraEmerald)
                        teamMessages.takeLast(4).forEach { m ->
                            Text("${m.senderName}: ${m.text}", style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
                        }
                        OutlinedTextField(
                            value = teamDraft,
                            onValueChange = { teamDraft = it },
                            label = { Text("Message team...") },
                            singleLine = true,
                            colors = auroraFieldColors()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (teamDraft.isNotBlank()) {
                            onSendTeamMessage(teamDraft)
                            teamDraft = ""
                        } else {
                            activeModal = AccountModal.NONE
                        }
                    }) { Text(if (teamDraft.isNotBlank()) "Send" else "Done", color = AuroraEmerald) }
                }
            )
        }

        AccountModal.SECURITY -> {
            val bioSupported = remember { canAuthenticateBiometric(context) }
            AlertDialog(
                onDismissRequest = { activeModal = AccountModal.NONE },
                containerColor = AuroraCardElevated,
                title = { Text("Security & Privacy", color = AuroraTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingSwitchRow(
                            label = if (bioSupported) "Biometric App Lock" else "Biometric Lock (Device unsupported)",
                            checked = localPrefs.biometricsEnabled,
                            enabled = bioSupported,
                            onCheckedChange = onSetBiometrics
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
