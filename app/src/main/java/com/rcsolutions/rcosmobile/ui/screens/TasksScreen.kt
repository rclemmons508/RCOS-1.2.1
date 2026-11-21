package com.rcsolutions.rcosmobile.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ForwardToInbox
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.FieldTask
import com.rcsolutions.rcosmobile.model.TaskBucket
import com.rcsolutions.rcosmobile.model.TaskCategory
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.util.Locale

enum class TasksFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    COMPLETED("Completed"),
    APPROVALS("Approvals")
}

@Composable
fun TasksScreen(
    tasks: List<FieldTask>,
    actions: List<ActionRecord>,
    initialFilter: TasksFilter = TasksFilter.TODAY,
    onAddTask: (title: String, subtitle: String, dueTime: String, bucket: TaskBucket, category: TaskCategory) -> Unit,
    onToggleTask: (FieldTask) -> Unit,
    onDecideApproval: (action: ActionRecord, approved: Boolean, editedDraft: String?, editedAmount: Double?) -> Unit,
    onMarkInvoicePaid: (String) -> Unit,
    onOpenCreateQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionRecord?>(null) }

    val pendingCount = actions.count { it.status == ActionStatus.AWAITING_APPROVAL.code }

    val visibleTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            TasksFilter.TODAY -> tasks.filter { !it.completed && it.bucket == TaskBucket.TODAY.code }
            TasksFilter.THIS_WEEK -> tasks.filter { !it.completed }
            TasksFilter.COMPLETED -> tasks.filter { it.completed }
            TasksFilter.APPROVALS -> emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (selectedFilter == TasksFilter.APPROVALS) "Approvals & Quotes" else "Tasks",
            style = MaterialTheme.typography.headlineMedium,
            color = AuroraTextPrimary
        )
        Text(
            text = if (selectedFilter == TasksFilter.APPROVALS) {
                "Human-in-the-loop safety sign-off."
            } else {
                "Stay organized. Get more done."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TasksFilter.entries.forEach { filter ->
                val pillLabel = if (filter == TasksFilter.APPROVALS && pendingCount > 0) {
                    "Approvals ($pendingCount)"
                } else filter.label
                AuroraFilterPill(
                    label = pillLabel,
                    selected = selectedFilter == filter,
                    testTag = "tasks_filter_${filter.name.lowercase()}",
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
            if (selectedFilter == TasksFilter.APPROVALS) {
                if (actions.isEmpty()) {
                    item {
                        AuroraEmptyState(
                            icon = Icons.Outlined.Description,
                            title = "Approval queue is clear",
                            subtitle = "Quotes, outbound messages, and invoices waiting for your review appear here.",
                            actionLabel = "Create Quote",
                            onActionClick = onOpenCreateQuote
                        )
                    }
                } else {
                    items(actions, key = { it.id }) { action ->
                        val awaiting = action.status == ActionStatus.AWAITING_APPROVAL.code
                        AuroraCard(
                            highlighted = awaiting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AuroraTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = when (action.status) {
                                        ActionStatus.AWAITING_APPROVAL.code -> AuroraAmber.copy(alpha = 0.2f)
                                        ActionStatus.APPROVED.code, ActionStatus.COMPLETED.code -> AuroraEmerald.copy(alpha = 0.2f)
                                        else -> AuroraRed.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = ActionStatus.fromCode(action.status).label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (action.status) {
                                            ActionStatus.AWAITING_APPROVAL.code -> AuroraAmber
                                            ActionStatus.APPROVED.code, ActionStatus.COMPLETED.code -> AuroraEmerald
                                            else -> AuroraRed
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (action.clientName.isNotBlank() || action.dollarAmount > 0.0) {
                                Spacer(Modifier.height(4.dp))
                                val amt = if (action.dollarAmount > 0.0) {
                                    " • $" + String.format(Locale.US, "%,.2f", action.dollarAmount)
                                } else ""
                                Text(
                                    text = "${action.clientName.ifBlank { "Client" }}$amt",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AuroraEmerald
                                )
                            }
                            if (action.draftContent.isNotBlank() || action.summary.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = action.draftContent.ifBlank { action.summary },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextSecondary,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (awaiting) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onDecideApproval(action, true, null, null) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AuroraEmerald,
                                            contentColor = AuroraOnEmerald
                                        ),
                                        modifier = Modifier.weight(1f).testTag("approve_action_${action.id}")
                                    ) {
                                        Text("Approve", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { editingAction = action },
                                        border = BorderStroke(1.dp, AuroraBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Edit", color = AuroraTextPrimary)
                                    }
                                    OutlinedButton(
                                        onClick = { onDecideApproval(action, false, null, null) },
                                        border = BorderStroke(1.dp, AuroraRed.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Decline", color = AuroraRed)
                                    }
                                }
                            } else if (action.actionType == "send_invoice" && action.paymentStatus != "paid") {
                                Spacer(Modifier.height(10.dp))
                                AuroraPrimaryCta(
                                    text = "Mark Invoice Paid",
                                    testTag = "mark_paid_${action.id}",
                                    onClick = { onMarkInvoicePaid(action.id) }
                                )
                            }
                        }
                    }
                }
            } else if (visibleTasks.isEmpty()) {
                item {
                    AuroraEmptyState(
                        icon = Icons.Outlined.TaskAlt,
                        title = "No tasks in ${selectedFilter.label}",
                        subtitle = "Add callbacks, quote follow-ups, or site checklists to stay ahead.",
                        actionLabel = "+ Add Task",
                        onActionClick = { showAddTaskDialog = true }
                    )
                }
            } else {
                items(visibleTasks, key = { it.id }) { task ->
                    val icon = when (TaskCategory.fromCode(task.category)) {
                        TaskCategory.CALL -> Icons.Outlined.Phone
                        TaskCategory.QUOTE -> Icons.Outlined.Description
                        TaskCategory.SCHEDULE -> Icons.Outlined.CalendarMonth
                        TaskCategory.FOLLOW_UP -> Icons.Outlined.ForwardToInbox
                        TaskCategory.JOB_CHECK -> Icons.Outlined.CheckCircle
                    }
                    AuroraCard(
                        onClick = { onToggleTask(task) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("task_card_${task.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AuroraEmerald.copy(alpha = 0.14f))
                                    .border(1.dp, AuroraEmerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = AuroraEmerald, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (task.completed) AuroraTextMuted else AuroraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (task.subtitle.isNotBlank()) {
                                    Text(
                                        text = task.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AuroraTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (task.dueTimeLabel.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.AccessTime,
                                            contentDescription = null,
                                            tint = AuroraTextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = task.dueTimeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AuroraTextMuted
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (task.completed) AuroraEmerald else AuroraCardElevated)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (task.completed) AuroraEmerald else AuroraTextMuted,
                                        shape = CircleShape
                                    )
                                    .clickable { onToggleTask(task) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.completed) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Completed", tint = AuroraOnEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        AuroraPrimaryCta(
            text = if (selectedFilter == TasksFilter.APPROVALS) "Create Quote" else "Add Task",
            icon = Icons.Outlined.Add,
            testTag = "tasks_primary_cta",
            onClick = {
                if (selectedFilter == TasksFilter.APPROVALS) onOpenCreateQuote()
                else showAddTaskDialog = true
            }
        )
    }

    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var due by remember { mutableStateOf("10:00 AM") }
        var bucket by remember { mutableStateOf(TaskBucket.TODAY) }
        var category by remember { mutableStateOf(TaskCategory.CALL) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("New Field Task", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Call back new lead") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Customer / Address / Phone") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    OutlinedTextField(
                        value = due,
                        onValueChange = { due = it },
                        label = { Text("Due Time") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskBucket.entries.forEach { b ->
                            AuroraFilterPill(
                                label = b.label,
                                selected = bucket == b,
                                testTag = "add_task_bucket_${b.code}",
                                onClick = { bucket = b },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TaskCategory.CALL, TaskCategory.QUOTE, TaskCategory.FOLLOW_UP).forEach { c ->
                            AuroraFilterPill(
                                label = c.label,
                                selected = category == c,
                                testTag = "add_task_cat_${c.code}",
                                onClick = { category = c },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onAddTask(title, subtitle, due, bucket, category)
                        showAddTaskDialog = false
                    }
                }) {
                    Text("Save Task", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }

    if (editingAction != null) {
        val current = editingAction!!
        var draftText by remember(current.id) { mutableStateOf(current.draftContent.ifBlank { current.summary }) }
        var amountText by remember(current.id) { mutableStateOf(if (current.dollarAmount > 0.0) current.dollarAmount.toString() else "0") }

        AlertDialog(
            onDismissRequest = { editingAction = null },
            containerColor = AuroraCardElevated,
            title = { Text("Edit & Approve", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount ($)") },
                        singleLine = true,
                        colors = auroraFieldColors()
                    )
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        label = { Text("Draft Message / Scope") },
                        minLines = 3,
                        colors = auroraFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsedAmt = amountText.toDoubleOrNull() ?: current.dollarAmount
                    onDecideApproval(current, true, draftText, parsedAmt)
                    editingAction = null
                }) {
                    Text("Save & Approve", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAction = null }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }
}
