package com.rcsolutions.rcosmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobPriority
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraCyan
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EventsDispatchFilter(val label: String) {
    ALL("All"),
    DISPATCH("Job Dispatch"),
    SYSTEM("System Events")
}

private sealed interface LiveFeedEntry {
    val key: String
    val sortEpochSeconds: Long
    val isDispatch: Boolean

    data class DispatchUpdate(
        override val key: String,
        override val sortEpochSeconds: Long,
        val job: Job
    ) : LiveFeedEntry {
        override val isDispatch: Boolean = true
    }

    data class SystemAuditEvent(
        override val key: String,
        override val sortEpochSeconds: Long,
        val audit: AuditLog
    ) : LiveFeedEntry {
        override val isDispatch: Boolean = false
    }

    data class SystemAlertEvent(
        override val key: String,
        override val sortEpochSeconds: Long,
        val alert: AgentInboxMessage
    ) : LiveFeedEntry {
        override val isDispatch: Boolean = false
    }

    data class SystemActionEvent(
        override val key: String,
        override val sortEpochSeconds: Long,
        val action: ActionRecord
    ) : LiveFeedEntry {
        override val isDispatch: Boolean = false
    }
}

@Composable
fun SystemEventsDispatchSection(
    jobsState: UiState<List<Job>>,
    auditLogsState: UiState<List<AuditLog>>,
    agentMessagesState: UiState<List<AgentInboxMessage>>,
    actionsState: UiState<List<ActionRecord>>,
    isSyncing: Boolean,
    lastSyncedLabel: String?,
    syncError: String?,
    onRefresh: () -> Unit,
    onUpdateJobStatus: (Job, JobStatus) -> Unit,
    onDismissAgentMessage: (String) -> Unit,
    onNavigateSchedule: () -> Unit,
    onNavigateApprovals: () -> Unit,
    onNavigateCalls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(EventsDispatchFilter.ALL) }
    var showExpanded by remember { mutableStateOf(false) }

    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()
    val agentMessages = (agentMessagesState as? UiState.Success)?.data.orEmpty()
    val actions = (actionsState as? UiState.Success)?.data.orEmpty()

    val isLoading = (jobsState is UiState.Loading || auditLogsState is UiState.Loading) &&
        jobs.isEmpty() && auditLogs.isEmpty() && agentMessages.isEmpty() && actions.isEmpty()

    val streamError = remember(jobsState, auditLogsState, agentMessagesState, actionsState, syncError) {
        syncError
            ?: (jobsState as? UiState.Error)?.message
            ?: (auditLogsState as? UiState.Error)?.message
            ?: (agentMessagesState as? UiState.Error)?.message
            ?: (actionsState as? UiState.Error)?.message
    }

    val allEntries = remember(jobs, auditLogs, agentMessages, actions) {
        buildList {
            jobs.forEach { job ->
                val ts = job.updatedAt?.seconds ?: job.createdAt?.seconds ?: 0L
                add(LiveFeedEntry.DispatchUpdate(key = "dispatch_${job.id}", sortEpochSeconds = ts, job = job))
            }
            auditLogs.forEach { audit ->
                val ts = audit.createdAt?.seconds ?: 0L
                add(LiveFeedEntry.SystemAuditEvent(key = "audit_${audit.id}", sortEpochSeconds = ts, audit = audit))
            }
            agentMessages.filter { it.status != "dismissed" }.forEach { alert ->
                val ts = alert.updatedAt?.seconds ?: alert.createdAt?.seconds ?: 0L
                add(LiveFeedEntry.SystemAlertEvent(key = "alert_${alert.id}", sortEpochSeconds = ts, alert = alert))
            }
            actions.filter {
                it.status in setOf(
                    ActionStatus.AWAITING_APPROVAL.code,
                    ActionStatus.QUEUED.code,
                    ActionStatus.RUNNING.code,
                    ActionStatus.FAILED.code
                )
            }.forEach { action ->
                val ts = action.updatedAt?.seconds ?: action.createdAt?.seconds ?: 0L
                add(LiveFeedEntry.SystemActionEvent(key = "action_${action.id}", sortEpochSeconds = ts, action = action))
            }
        }.sortedByDescending { it.sortEpochSeconds }
    }

    val dispatchCount = remember(allEntries) { allEntries.count { it.isDispatch } }
    val systemCount = remember(allEntries) { allEntries.count { !it.isDispatch } }

    val visibleEntries = remember(allEntries, selectedFilter, showExpanded) {
        val filtered = when (selectedFilter) {
            EventsDispatchFilter.ALL -> allEntries
            EventsDispatchFilter.DISPATCH -> allEntries.filter { it.isDispatch }
            EventsDispatchFilter.SYSTEM -> allEntries.filter { !it.isDispatch }
        }
        if (showExpanded) filtered.take(12) else filtered.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_events_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header + Live Firestore Sync Status + Refresh Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "System & Dispatch Feed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AuroraTextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AuroraEmerald.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (streamError != null) AuroraRed else AuroraEmerald)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isSyncing -> "Syncing"
                                    lastSyncedLabel != null -> lastSyncedLabel
                                    else -> "Firestore Live"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (streamError != null) AuroraRed else AuroraEmerald
                            )
                        }
                    }
                }
                Text(
                    text = "Real-time system alerts, approvals & crew dispatch updates",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isSyncing,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_events_refresh_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = AuroraEmerald,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh system events and dispatch updates",
                        tint = AuroraEmerald
                    )
                }
            }
        }

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventsDispatchFilter.entries.forEach { filter ->
                val countSuffix = when (filter) {
                    EventsDispatchFilter.ALL -> if (allEntries.isNotEmpty()) " (${allEntries.size})" else ""
                    EventsDispatchFilter.DISPATCH -> if (dispatchCount > 0) " ($dispatchCount)" else ""
                    EventsDispatchFilter.SYSTEM -> if (systemCount > 0) " ($systemCount)" else ""
                }
                AuroraFilterPill(
                    label = "${filter.label}$countSuffix",
                    selected = selectedFilter == filter,
                    testTag = "home_events_filter_${filter.name.lowercase()}",
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Firestore Error State
        AnimatedVisibility(visible = streamError != null) {
            AuroraCard(
                highlighted = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Sync error",
                        tint = AuroraRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unable to refresh Firestore feed",
                            style = MaterialTheme.typography.titleSmall,
                            color = AuroraTextPrimary
                        )
                        Text(
                            text = streamError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = AuroraTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_events_retry_button")
                    ) {
                        Text("Retry", color = AuroraEmerald)
                    }
                }
            }
        }

        // Loading / Empty / Live Feed Cards
        when {
            isLoading -> {
                AuroraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AuroraEmerald,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Fetching system events & dispatch updates...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextSecondary
                        )
                    }
                }
            }

            visibleEntries.isEmpty() -> {
                AuroraEmptyState(
                    icon = if (selectedFilter == EventsDispatchFilter.DISPATCH) {
                        Icons.Outlined.LocalShipping
                    } else {
                        Icons.Outlined.NotificationsActive
                    },
                    title = when (selectedFilter) {
                        EventsDispatchFilter.DISPATCH -> "No job dispatch updates yet"
                        EventsDispatchFilter.SYSTEM -> "No system events recorded"
                        EventsDispatchFilter.ALL -> "No system or dispatch updates yet"
                    },
                    subtitle = "Scheduled crew dispatches, approval decisions, and AI system alerts sync here in real time.",
                    actionLabel = "Open Dispatch Schedule",
                    onActionClick = onNavigateSchedule
                )
            }

            else -> {
                visibleEntries.forEach { entry ->
                    when (entry) {
                        is LiveFeedEntry.DispatchUpdate -> {
                            DispatchUpdateCard(
                                job = entry.job,
                                timestampLabel = formatEventTimestamp(entry.sortEpochSeconds),
                                onOpenSchedule = onNavigateSchedule,
                                onAdvanceStatus = { next -> onUpdateJobStatus(entry.job, next) }
                            )
                        }

                        is LiveFeedEntry.SystemAuditEvent -> {
                            SystemEventCard(
                                testTag = "home_event_item_${entry.key}",
                                icon = Icons.Outlined.Gavel,
                                iconTint = if (entry.audit.decision == ActionStatus.REJECTED.code) AuroraRed else AuroraEmerald,
                                badgeLabel = entry.audit.decision.replaceFirstChar { it.uppercase() }.ifBlank { "Audit" },
                                badgeColor = if (entry.audit.decision == ActionStatus.REJECTED.code) AuroraRed else AuroraEmerald,
                                title = entry.audit.summary.ifBlank { entry.audit.actionType.replace("_", " ") },
                                subtitle = "Policy log • ${entry.audit.actionType.replace("_", " ")}",
                                timeLabel = formatEventTimestamp(entry.sortEpochSeconds),
                                onClick = onNavigateApprovals
                            )
                        }

                        is LiveFeedEntry.SystemAlertEvent -> {
                            val tint = when (entry.alert.kind) {
                                "alert" -> AuroraAmber
                                "question" -> AuroraCyan
                                else -> AuroraEmerald
                            }
                            SystemEventCard(
                                testTag = "home_event_item_${entry.key}",
                                icon = if (entry.alert.kind == "alert") Icons.Outlined.WarningAmber else Icons.Outlined.AutoAwesome,
                                iconTint = tint,
                                badgeLabel = when (entry.alert.kind) {
                                    "alert" -> "System Alert"
                                    "question" -> "Action Needed"
                                    else -> "AI Update"
                                },
                                badgeColor = tint,
                                title = entry.alert.title,
                                subtitle = entry.alert.body,
                                timeLabel = formatEventTimestamp(entry.sortEpochSeconds),
                                onDismiss = { onDismissAgentMessage(entry.alert.id) },
                                onClick = {
                                    when (entry.alert.actionRoute.lowercase()) {
                                        "schedule" -> onNavigateSchedule()
                                        "calls" -> onNavigateCalls()
                                        else -> onNavigateApprovals()
                                    }
                                }
                            )
                        }

                        is LiveFeedEntry.SystemActionEvent -> {
                            val status = ActionStatus.fromCode(entry.action.status)
                            val badgeColor = when (status) {
                                ActionStatus.FAILED, ActionStatus.REJECTED -> AuroraRed
                                ActionStatus.AWAITING_APPROVAL -> AuroraAmber
                                ActionStatus.RUNNING, ActionStatus.QUEUED -> AuroraCyan
                                else -> AuroraEmerald
                            }
                            val amountSuffix = if (entry.action.dollarAmount > 0.0) {
                                " • $${entry.action.dollarAmount.toInt()}"
                            } else ""
                            SystemEventCard(
                                testTag = "home_event_item_${entry.key}",
                                icon = Icons.Outlined.NotificationsActive,
                                iconTint = badgeColor,
                                badgeLabel = status.label,
                                badgeColor = badgeColor,
                                title = entry.action.title,
                                subtitle = "${entry.action.clientName.ifBlank { "Workspace Queue" }}$amountSuffix",
                                timeLabel = formatEventTimestamp(entry.sortEpochSeconds),
                                onClick = onNavigateApprovals
                            )
                        }
                    }
                }

                val filteredTotal = when (selectedFilter) {
                    EventsDispatchFilter.ALL -> allEntries.size
                    EventsDispatchFilter.DISPATCH -> dispatchCount
                    EventsDispatchFilter.SYSTEM -> systemCount
                }
                if (filteredTotal > 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { showExpanded = !showExpanded },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_events_toggle_expand_button")
                        ) {
                            Text(
                                text = if (showExpanded) "Show Fewer Updates" else "Show More Updates ($filteredTotal total)",
                                color = AuroraEmerald,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchUpdateCard(
    job: Job,
    timestampLabel: String,
    onOpenSchedule: () -> Unit,
    onAdvanceStatus: (JobStatus) -> Unit
) {
    val jobStatus = JobStatus.fromCode(job.status)
    val isCritical = job.priority == JobPriority.CRITICAL.code || jobStatus == JobStatus.URGENT
    val statusColor = when (jobStatus) {
        JobStatus.URGENT -> AuroraRed
        JobStatus.IN_PROGRESS -> AuroraEmerald
        JobStatus.DISPATCHED -> AuroraCyan
        JobStatus.UNASSIGNED -> AuroraAmber
        JobStatus.COMPLETED -> AuroraTextMuted
    }
    val nextAction: Pair<String, JobStatus>? = when (jobStatus) {
        JobStatus.UNASSIGNED, JobStatus.URGENT -> "Dispatch" to JobStatus.DISPATCHED
        JobStatus.DISPATCHED -> "Start Job" to JobStatus.IN_PROGRESS
        JobStatus.IN_PROGRESS -> "Complete" to JobStatus.COMPLETED
        JobStatus.COMPLETED -> null
    }

    AuroraCard(
        highlighted = isCritical || jobStatus == JobStatus.IN_PROGRESS,
        onClick = onOpenSchedule,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_event_item_job_${job.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (jobStatus == JobStatus.COMPLETED) Icons.Outlined.CheckCircle else Icons.Outlined.LocalShipping,
                    contentDescription = "Job dispatch status",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(label = jobStatus.label, color = statusColor)
                    if (timestampLabel.isNotBlank()) {
                        Text(
                            text = timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraTextMuted,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${job.title} — ${job.clientName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = AuroraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val techLabel = job.assignedTechnician.ifBlank { "Needs tech" }
                val schedLabel = listOf(job.scheduledDate, job.scheduledTime).filter { it.isNotBlank() }.joinToString(" • ")
                Text(
                    text = listOfNotNull(
                        techLabel,
                        schedLabel.ifBlank { null },
                        job.address.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (nextAction != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AuroraEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .minimumInteractiveComponentSize()
                        .testTag("home_dispatch_advance_${job.id}")
                        .clickable { onAdvanceStatus(nextAction.second) }
                ) {
                    Text(
                        text = nextAction.first,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AuroraEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open schedule",
                    tint = AuroraTextMuted
                )
            }
        }
    }
}

@Composable
private fun SystemEventCard(
    testTag: String,
    icon: ImageVector,
    iconTint: Color,
    badgeLabel: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    timeLabel: String,
    onDismiss: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    AuroraCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = badgeLabel,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(label = badgeLabel, color = badgeColor)
                    if (timeLabel.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = AuroraTextMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraTextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AuroraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTag}_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss system alert",
                        tint = AuroraTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open details",
                    tint = AuroraTextMuted
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

private fun formatEventTimestamp(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "Just now"
    val nowSec = System.currentTimeMillis() / 1000L
    val diffSec = (nowSec - epochSeconds).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86400L -> "${diffSec / 3600L}h ago"
        else -> SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(epochSeconds * 1000L))
    }
}
