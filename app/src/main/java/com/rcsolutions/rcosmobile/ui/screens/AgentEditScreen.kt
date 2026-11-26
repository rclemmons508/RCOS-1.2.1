package com.rcsolutions.rcosmobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.AIEmployee
import com.rcsolutions.rcosmobile.model.AgentSettings
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.ui.components.AuroraBackground
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary

/**
 * Edit one AI agent: what it is, what it is allowed to do
 * (permissions), and when it acts on its own vs escalates to the
 * owner (thresholds). Settings are stored per agent on the business
 * doc (agentSettings.<employeeId>); an agent the owner has never
 * edited reads the defaults from RcosDefaults.
 */
@Composable
fun AgentEditScreen(
    business: BusinessAccount?,
    employee: AIEmployee,
    onSave: (AgentSettings) -> Unit,
    onClose: () -> Unit,
) {
    val current = remember(employee.id, business?.id) {
        RcosDefaults.agentSettingsFor(business, employee.id)
    }
    var selectedPermissions by remember { mutableStateOf(current.permissions.toSet()) }
    var autoActText by remember { mutableStateOf(formatMoney(current.autoActLimit)) }
    var escalateText by remember { mutableStateOf(formatMoney(current.escalateAbove)) }
    var requireApprovalForMoney by remember { mutableStateOf(current.requireApprovalForMoney) }
    var customInstructions by remember { mutableStateOf(current.customInstructions) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("‹ Back", color = AuroraTextSecondary)
                }
                TextButton(onClick = onClose) {
                    Text("Close", color = AuroraTextSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = employee.name,
                style = MaterialTheme.typography.headlineMedium,
                color = AuroraTextPrimary
            )
            Text(
                text = "${employee.department} · ${employee.tagline}",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary
            )
            Spacer(Modifier.height(14.dp))

            // ---- What this agent does ----
            AuroraCard {
                Text("What this agent does", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(employee.coreJob, style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Escalates to: ${employee.escalatesTo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextMuted
                )
                Text(
                    text = "Default tone: ${employee.defaultTone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            // ---- Permissions ----
            Text("Permissions — what this agent is allowed to do", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
            Spacer(Modifier.height(8.dp))
            AuroraCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RcosDefaults.AGENT_PERMISSIONS.forEach { permission ->
                        val allowed = permission.id in selectedPermissions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(permission.label, style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                                Text(permission.description, style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                            }
                            Switch(
                                checked = allowed,
                                onCheckedChange = {
                                    selectedPermissions = if (it) selectedPermissions + permission.id
                                    else selectedPermissions - permission.id
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuroraOnEmerald,
                                    checkedTrackColor = AuroraEmerald
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Thresholds ----
            Text("Thresholds — when it acts vs escalates", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
            Spacer(Modifier.height(8.dp))
            AuroraCard {
                OutlinedTextField(
                    value = autoActText,
                    onValueChange = { autoActText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Act on its own under ($)") },
                    placeholder = { Text("250") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("agent_auto_act_limit"),
                    colors = auroraFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = escalateText,
                    onValueChange = { escalateText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Escalate to owner above ($)") },
                    placeholder = { Text("1000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("agent_escalate_above"),
                    colors = auroraFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Below the first amount the agent acts alone; above the second it asks you first. The escalate amount is never lower than the act amount.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Money moves need owner approval", style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                        Text("Invoices and payments are never sent without your sign-off.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                    }
                    Switch(
                        checked = requireApprovalForMoney,
                        onCheckedChange = { requireApprovalForMoney = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AuroraOnEmerald,
                            checkedTrackColor = AuroraEmerald
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Custom instructions ----
            Text("Instructions for this agent", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
            Spacer(Modifier.height(8.dp))
            AuroraCard {
                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    label = { Text("Anything this agent should always do, or never do") },
                    placeholder = { Text("e.g. Never quote weekend rates without checking the calendar first.") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("agent_custom_instructions"),
                    colors = auroraFieldColors()
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AuroraSecondaryCta(
                    text = "Cancel",
                    testTag = "agent_edit_cancel",
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                )
                AuroraPrimaryCta(
                    text = "Save Agent",
                    testTag = "agent_edit_save",
                    onClick = {
                        val auto = autoActText.toDoubleOrNull() ?: current.autoActLimit
                        val escalate = escalateText.toDoubleOrNull() ?: current.escalateAbove
                        onSave(
                            AgentSettings(
                                permissions = RcosDefaults.AGENT_PERMISSIONS
                                    .map { it.id }
                                    .filter { it in selectedPermissions },
                                autoActLimit = auto,
                                escalateAbove = maxOf(escalate, auto),
                                requireApprovalForMoney = requireApprovalForMoney,
                                customInstructions = customInstructions
                            )
                        )
                        onClose()
                    },
                    modifier = Modifier.weight(1.5f)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatMoney(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
