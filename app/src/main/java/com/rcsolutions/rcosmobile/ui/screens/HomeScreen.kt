package com.rcsolutions.rcosmobile.ui.screens

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.model.WeatherSnapshot
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraSectionHeader
import com.rcsolutions.rcosmobile.ui.components.RcSolutionsBrandLockup
import com.rcsolutions.rcosmobile.ui.components.SystemEventsDispatchSection
import com.rcsolutions.rcosmobile.ui.components.WeeklyDispatchTrendsCard
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ActivityFeedEntry(
    val id: String,
    val title: String,
    val timeAgo: String,
    val icon: ImageVector,
    val routeTarget: String,
    val sortEpoch: Long
)

@Composable
fun HomeScreen(
    ownerFirstName: String,
    business: BusinessAccount?,
    weather: WeatherSnapshot?,
    focusQuote: String,
    calls: List<CallRecord>,
    actions: List<ActionRecord>,
    jobs: List<Job>,
    agentMessages: List<AgentInboxMessage>,
    jobsState: UiState<List<Job>>,
    auditLogsState: UiState<List<AuditLog>>,
    agentMessagesState: UiState<List<AgentInboxMessage>>,
    actionsState: UiState<List<ActionRecord>>,
    isOperationalSyncing: Boolean,
    lastOperationalSyncLabel: String?,
    operationalSyncError: String?,
    weeklyDispatchJobsState: UiState<List<Job>>,
    isWeeklyTrendsRefreshing: Boolean,
    weeklyTrendsSyncLabel: String?,
    weeklyTrendsError: String?,
    onRefreshOperationalFeeds: () -> Unit,
    onRefreshWeeklyTrends: () -> Unit,
    onUpdateJobStatus: (Job, JobStatus) -> Unit,
    onDismissAgentMessage: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onUpdateFocusQuote: (String) -> Unit,
    onUpdateWeatherCity: (String) -> Unit,
    onNavigateCalls: () -> Unit,
    onOpenCreateQuote: () -> Unit,
    onNavigateSchedule: () -> Unit,
    onNavigateCustomers: () -> Unit,
    onNavigateTasks: () -> Unit,
    onNavigateApprovals: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuoteDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showAllActionsSheet by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val todayLabel = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(Date())
    }
    val unreadAlerts = remember(agentMessages, actions) {
        agentMessages.count { it.status == "unread" } +
            actions.count { it.status == "awaiting_approval" }
    }

    val recentFeed = remember(calls, actions, jobs) {
        val items = mutableListOf<ActivityFeedEntry>()
        calls.forEach { call ->
            items += ActivityFeedEntry(
                id = "call_${call.id}",
                title = "${call.callerName} • ${call.callerNumber}",
                timeAgo = call.timeLabel.ifBlank { "Recent call" },
                icon = Icons.Outlined.PhoneInTalk,
                routeTarget = "calls",
                sortEpoch = call.updatedAt?.seconds ?: call.createdAt?.seconds ?: 0L
            )
        }
        actions.forEach { act ->
            items += ActivityFeedEntry(
                id = "act_${act.id}",
                title = act.title,
                timeAgo = act.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                icon = Icons.Outlined.Description,
                routeTarget = "approvals",
                sortEpoch = act.updatedAt?.seconds ?: act.createdAt?.seconds ?: 0L
            )
        }
        jobs.forEach { job ->
            items += ActivityFeedEntry(
                id = "job_${job.id}",
                title = "${job.title} — ${job.clientName}",
                timeAgo = "${job.scheduledDate} • ${job.scheduledTime}",
                icon = Icons.Outlined.CalendarMonth,
                routeTarget = "schedule",
                sortEpoch = job.updatedAt?.seconds ?: job.createdAt?.seconds ?: 0L
            )
        }
        items.sortedByDescending { it.sortEpoch }.take(8)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Brand Lockup + Notification Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RcSolutionsBrandLockup(compact = true)
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadAlerts > 0) {
                                Badge(containerColor = AuroraEmerald) {
                                    Text(
                                        text = unreadAlerts.coerceAtMost(99).toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications and alerts",
                            tint = AuroraTextPrimary
                        )
                    }
                }
            }
        }

        // 2. Greeting + Live Date (Left) | Weather / City Card (Right)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$greeting\n${ownerFirstName.ifBlank { "Owner" }}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 27.sp,
                            lineHeight = 32.sp
                        ),
                        color = AuroraTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = todayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                }

                Spacer(Modifier.width(12.dp))
                AuroraCard(
                    onClick = { showCityDialog = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("home_weather_card")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (weather?.isClear != false) Icons.Outlined.WbSunny else Icons.Outlined.WbCloudy,
                            contentDescription = "Weather condition",
                            tint = if (weather?.isClear != false) AuroraAmber else AuroraEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        if (weather != null) {
                            Text(
                                text = "${weather.temperatureFahrenheit}°",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AuroraTextPrimary
                            )
                            Text(
                                text = weather.conditionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraTextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = weather.locationLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = AuroraTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = business?.serviceArea?.ifBlank { "Set City" } ?: "Set City",
                                style = MaterialTheme.typography.labelMedium,
                                color = AuroraEmerald,
                                maxLines = 1
                            )
                            Text(
                                text = "Live Weather",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Focus Quote Banner (+ button)
        item {
            AuroraCard(
                highlighted = true,
                onClick = { showQuoteDialog = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_focus_quote_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AuroraEmerald.copy(alpha = 0.16f))
                            .border(1.dp, AuroraEmerald.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Edit focus quote",
                            tint = AuroraEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = focusQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AuroraTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Quick Actions 3x2 Grid (Matches Screen 1)
        item {
            AuroraSectionHeader(
                title = "Quick Actions",
                actionLabel = "See All",
                onActionClick = { showAllActionsSheet = true }
            )
            Spacer(Modifier.height(10.dp))
            val pendingApprovalsCount = actions.count { it.status == "awaiting_approval" }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = "Call AI\nReceptionist",
                        icon = Icons.Outlined.PhoneInTalk,
                        testTag = "qa_calls",
                        onClick = onNavigateCalls,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = "Create\nQuote",
                        icon = Icons.Outlined.Description,
                        testTag = "qa_create_quote",
                        onClick = onOpenCreateQuote,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = "Schedule\nService",
                        icon = Icons.Outlined.CalendarMonth,
                        testTag = "qa_schedule",
                        onClick = onNavigateSchedule,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        label = "View\nCustomers",
                        icon = Icons.Outlined.Groups,
                        testTag = "qa_customers",
                        onClick = onNavigateCustomers,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = "Open\nTasks",
                        icon = Icons.Outlined.CheckCircle,
                        testTag = "qa_tasks",
                        onClick = onNavigateTasks,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        label = if (pendingApprovalsCount > 0) "Approvals\n($pendingApprovalsCount)" else "Approvals\n& Queue",
                        icon = Icons.Outlined.Info,
                        badgeHighlight = pendingApprovalsCount > 0,
                        testTag = "qa_approvals",
                        onClick = onNavigateApprovals,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Weekly Job Dispatch Trends Chart (Vico + Firestore)
        item {
            WeeklyDispatchTrendsCard(
                jobsState = weeklyDispatchJobsState,
                isRefreshing = isWeeklyTrendsRefreshing,
                syncLabel = weeklyTrendsSyncLabel,
                syncError = weeklyTrendsError,
                onRefreshFromFirestore = onRefreshWeeklyTrends,
                onOpenSchedule = onNavigateSchedule
            )
        }

        // 6. Live Firestore System Events & Job Dispatch Updates
        item {
            SystemEventsDispatchSection(
                jobsState = jobsState,
                auditLogsState = auditLogsState,
                agentMessagesState = agentMessagesState,
                actionsState = actionsState,
                isSyncing = isOperationalSyncing,
                lastSyncedLabel = lastOperationalSyncLabel,
                syncError = operationalSyncError,
                onRefresh = onRefreshOperationalFeeds,
                onUpdateJobStatus = onUpdateJobStatus,
                onDismissAgentMessage = onDismissAgentMessage,
                onNavigateSchedule = onNavigateSchedule,
                onNavigateApprovals = onNavigateApprovals,
                onNavigateCalls = onNavigateCalls
            )
        }

        // 7. Recent Activity Feed
        item {
            AuroraSectionHeader(
                title = "Recent Activity",
                actionLabel = "See All",
                onActionClick = onNavigateApprovals
            )
        }

        if (recentFeed.isEmpty()) {
            item {
                AuroraEmptyState(
                    icon = Icons.Outlined.Description,
                    title = "Ready for today's jobs",
                    subtitle = "Create a quote, add a customer, or schedule an appointment to see live activity here.",
                    actionLabel = "Create First Quote",
                    onActionClick = onOpenCreateQuote
                )
            }
        } else {
            items(recentFeed, key = { it.id }) { entry ->
                AuroraCard(
                    onClick = {
                        when (entry.routeTarget) {
                            "calls" -> onNavigateCalls()
                            "schedule" -> onNavigateSchedule()
                            else -> onNavigateApprovals()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuroraEmerald.copy(alpha = 0.14f))
                                .border(1.dp, AuroraEmerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                                tint = AuroraEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = AuroraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = entry.timeAgo,
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraTextSecondary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Open item",
                            tint = AuroraTextMuted
                        )
                    }
                }
            }
        }
    }

    if (showQuoteDialog) {
        var draftQuote by remember { mutableStateOf(focusQuote) }
        AlertDialog(
            onDismissRequest = { showQuoteDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Daily Focus Banner", color = AuroraTextPrimary) },
            text = {
                OutlinedTextField(
                    value = draftQuote,
                    onValueChange = { draftQuote = it },
                    label = { Text("Focus quote or team goal") },
                    minLines = 2,
                    colors = auroraFieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draftQuote.isNotBlank()) onUpdateFocusQuote(draftQuote)
                    showQuoteDialog = false
                }) {
                    Text("Save", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuoteDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }

    if (showCityDialog) {
        var cityInput by remember { mutableStateOf(business?.serviceArea.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Service Area Weather", color = AuroraTextPrimary) },
            text = {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City (e.g. Greenville, SC)") },
                    singleLine = true,
                    colors = auroraFieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateWeatherCity(cityInput)
                    showCityDialog = false
                }) {
                    Text("Update", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }

    if (showAllActionsSheet) {
        AlertDialog(
            onDismissRequest = { showAllActionsSheet = false },
            containerColor = AuroraCardElevated,
            title = { Text("Workspace Shortcuts", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showAllActionsSheet = false; onNavigateCustomers() }) {
                        Text("Customers & CRM Directory", color = AuroraEmerald)
                    }
                    TextButton(onClick = { showAllActionsSheet = false; onNavigateSchedule() }) {
                        Text("Schedule & Field Dispatch", color = AuroraEmerald)
                    }
                    TextButton(onClick = { showAllActionsSheet = false; onNavigateApprovals() }) {
                        Text("Approvals, Quotes & Invoices", color = AuroraEmerald)
                    }
                    TextButton(onClick = { showAllActionsSheet = false; onOpenCreateQuote() }) {
                        Text("New Quote with AI Estimator", color = AuroraEmerald)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllActionsSheet = false }) {
                    Text("Close", color = AuroraTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    badgeHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    AuroraCard(
        highlighted = badgeHighlight,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
        modifier = modifier
            .height(106.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuroraEmerald,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                ),
                color = AuroraTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
