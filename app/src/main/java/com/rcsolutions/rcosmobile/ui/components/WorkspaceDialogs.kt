package com.rcsolutions.rcosmobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.QuoteDraftSuggestion
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateQuoteOrInvoiceDialog(
    services: List<String>,
    prefillJob: Job? = null,
    isGeneratingAi: Boolean,
    isSaving: Boolean,
    aiSuggestion: QuoteDraftSuggestion?,
    onGenerateWithAi: (clientName: String, serviceType: String, scopeNotes: String) -> Unit,
    onClearAiSuggestion: () -> Unit,
    onSubmit: (
        clientName: String,
        title: String,
        summary: String,
        amount: Double,
        draftContent: String,
        isInvoice: Boolean,
        linkedJobId: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var isInvoice by remember(prefillJob) { mutableStateOf(prefillJob != null) }
    var clientName by remember(prefillJob) { mutableStateOf(prefillJob?.clientName.orEmpty()) }
    var serviceType by remember(prefillJob, services) {
        mutableStateOf(prefillJob?.category?.ifBlank { null } ?: services.firstOrNull() ?: "Service Estimate")
    }
    var scopeNotes by remember(prefillJob) { mutableStateOf(prefillJob?.description.orEmpty()) }
    var title by remember(prefillJob) {
        mutableStateOf(
            if (prefillJob != null) "Invoice: ${prefillJob.title}"
            else ""
        )
    }
    var amountText by remember(prefillJob) {
        mutableStateOf(
            if (prefillJob != null && prefillJob.estimatedValue > 0.0) {
                String.format(Locale.US, "%.2f", prefillJob.estimatedValue)
            } else ""
        )
    }
    var summary by remember(prefillJob) { mutableStateOf(prefillJob?.title.orEmpty()) }
    var draftContent by remember(prefillJob) {
        mutableStateOf(
            if (prefillJob != null) {
                "Hi ${prefillJob.clientName}, thank you for choosing us for ${prefillJob.title}. Attached is your final invoice."
            } else ""
        )
    }

    LaunchedEffect(aiSuggestion) {
        if (aiSuggestion != null) {
            if (aiSuggestion.title.isNotBlank()) title = aiSuggestion.title
            if (aiSuggestion.summary.isNotBlank()) summary = aiSuggestion.summary
            if (aiSuggestion.totalAmount > 0.0) {
                amountText = String.format(Locale.US, "%.2f", aiSuggestion.totalAmount)
            }
            if (aiSuggestion.clientMessage.isNotBlank()) {
                draftContent = buildString {
                    append(aiSuggestion.clientMessage)
                    if (aiSuggestion.lineItemsSummary.isNotBlank()) {
                        append("\n\nLine Items:\n")
                        append(aiSuggestion.lineItemsSummary)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            onClearAiSuggestion()
            onDismiss()
        },
        containerColor = AuroraCardElevated,
        title = {
            Text(
                text = if (isInvoice) "Create Invoice" else "Create Quote",
                color = AuroraTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isInvoice,
                        onClick = { isInvoice = false },
                        label = { Text("Quote") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuroraEmerald,
                            selectedLabelColor = AuroraOnEmerald
                        )
                    )
                    FilterChip(
                        selected = isInvoice,
                        onClick = { isInvoice = true },
                        label = { Text("Invoice") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuroraEmerald,
                            selectedLabelColor = AuroraOnEmerald
                        )
                    )
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Customer name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quote_client_input")
                )

                if (services.isNotEmpty()) {
                    Text(
                        text = "Service type",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuroraTextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        services.take(6).forEach { svc ->
                            FilterChip(
                                selected = serviceType == svc,
                                onClick = { serviceType = svc },
                                label = { Text(svc) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AuroraEmerald.copy(alpha = 0.22f),
                                    selectedLabelColor = AuroraEmerald
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    label = { Text("Service / Category *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = scopeNotes,
                    onValueChange = { scopeNotes = it },
                    label = { Text("Job scope notes (for AI estimator)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("quote_scope_input")
                )

                OutlinedButton(
                    onClick = { onGenerateWithAi(clientName, serviceType, scopeNotes) },
                    enabled = !isGeneratingAi && clientName.isNotBlank() && serviceType.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("quote_ai_estimate_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.6f))
                ) {
                    if (isGeneratingAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AuroraEmerald
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Estimating with Gemini...", color = AuroraEmerald)
                    } else {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AuroraEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("AI Estimate Scope & Draft", color = AuroraEmerald)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document title *") },
                    placeholder = { Text("e.g., Lawn Maintenance Quote - Johnson Residence") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quote_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Estimated amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quote_amount_input")
                )

                OutlinedTextField(
                    value = draftContent,
                    onValueChange = { draftContent = it },
                    label = { Text("Client message draft") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("quote_draft_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.trim().toDoubleOrNull() ?: 0.0
                    val effectiveTitle = title.trim().ifBlank {
                        "${if (isInvoice) "Invoice" else "Quote"}: $serviceType - $clientName"
                    }
                    onSubmit(
                        clientName.trim(),
                        effectiveTitle,
                        summary.trim().ifBlank { scopeNotes.trim() },
                        amount,
                        draftContent.trim(),
                        isInvoice,
                        prefillJob?.id.orEmpty()
                    )
                },
                enabled = !isSaving && clientName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraEmerald,
                    contentColor = AuroraOnEmerald
                ),
                modifier = Modifier.testTag("submit_quote_button")
            ) {
                Text(
                    text = if (isSaving) "Saving..." else if (isInvoice) "Queue Invoice" else "Queue Quote",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearAiSuggestion()
                    onDismiss()
                }
            ) {
                Text("Cancel", color = AuroraTextSecondary)
            }
        }
    )
}

@Composable
fun NotificationsCenterDialog(
    agentMessages: List<AgentInboxMessage>,
    pendingApprovalsCount: Int,
    onDismissMessage: (String) -> Unit,
    onRouteAction: (String) -> Unit,
    onOpenApprovals: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeMessages = remember(agentMessages) {
        agentMessages.filter { it.status != "dismissed" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AuroraCardElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = AuroraEmerald
                )
                Text(
                    text = "Workspace Alerts",
                    color = AuroraTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pendingApprovalsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AuroraAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AuroraAmber.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenApprovals()
                            }
                            .testTag("notifications_approvals_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$pendingApprovalsCount item(s) awaiting your approval",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AuroraAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to review quotes, invoices, and drafts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuroraTextSecondary
                                )
                            }
                        }
                    }
                }

                if (activeMessages.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AuroraCardSurface,
                        border = BorderStroke(1.dp, AuroraBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "All caught up",
                                style = MaterialTheme.typography.titleSmall,
                                color = AuroraTextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Proactive briefings from your AI team will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuroraTextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeMessages, key = { it.id }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AuroraCardSurface,
                                border = BorderStroke(1.dp, AuroraBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = msg.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AuroraTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onDismissMessage(msg.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Close,
                                                contentDescription = "Dismiss alert",
                                                tint = AuroraTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AuroraTextSecondary
                                    )
                                    if (msg.actionLabel.isNotBlank() && msg.actionRoute.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(
                                            onClick = {
                                                onDismissMessage(msg.id)
                                                onDismiss()
                                                onRouteAction(msg.actionRoute)
                                            }
                                        ) {
                                            Text(msg.actionLabel, color = AuroraEmerald)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AuroraEmerald)
            }
        }
    )
}
