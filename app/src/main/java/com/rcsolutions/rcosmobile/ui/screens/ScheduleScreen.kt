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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobPriority
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraDivider
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayCell(
    val isoDate: String,
    val dayOfWeekShort: String,
    val dayOfMonth: String
)

@Composable
fun ScheduleScreen(
    business: BusinessAccount?,
    jobs: List<Job>,
    onBack: (() -> Unit)? = null,
    onAddJob: (
        title: String,
        clientName: String,
        clientPhone: String,
        address: String,
        scheduledDate: String,
        scheduledTime: String,
        durationLabel: String,
        category: String,
        estimatedValue: Double,
        priority: JobPriority,
        description: String
    ) -> Unit,
    onUpdateJobStatus: (Job, JobStatus) -> Unit,
    onCreateInvoiceForJob: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }
    var selectedDateIso by remember { mutableStateOf(todayIso) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }

    val weekDays = remember(weekOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dowFmt = SimpleDateFormat("EEE", Locale.US)
        val domFmt = SimpleDateFormat("d", Locale.US)
        (0..6).map { idx ->
            val d = cal.time
            val cell = DayCell(
                isoDate = isoFmt.format(d),
                dayOfWeekShort = dowFmt.format(d).uppercase(),
                dayOfMonth = domFmt.format(d)
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cell
        }
    }

    val monthHeader = remember(weekOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    val dayJobs = remember(jobs, selectedDateIso) {
        val matching = jobs.filter { it.scheduledDate == selectedDateIso }
        if (matching.isEmpty() && selectedDateIso == todayIso) {
            jobs.filter { it.scheduledDate.isBlank() || it.scheduledDate == todayIso }
        } else matching
    }

    val jobPack = remember(business) {
        RcosDefaults.findPreset(business?.industry ?: "general_small_business").jobPack
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = AuroraTextPrimary)
                }
            }
            Column {
                Text("Schedule", style = MaterialTheme.typography.headlineMedium, color = AuroraTextPrimary)
                Text("Manage your appointments.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Calendar Card with Month < > + 7-day strip (Screen 6)
        AuroraCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthHeader,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AuroraTextPrimary
                )
                Row {
                    IconButton(onClick = { weekOffset-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous week", tint = AuroraTextSecondary)
                    }
                    IconButton(onClick = { weekOffset++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next week", tint = AuroraTextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { cell ->
                    val selected = cell.isoDate == selectedDateIso
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedDateIso = cell.isoDate }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cell.dayOfWeekShort,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) AuroraEmerald else AuroraTextMuted
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (selected) AuroraEmerald else AuroraCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cell.dayOfMonth,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) AuroraOnEmerald else AuroraTextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (dayJobs.isEmpty()) {
                item {
                    AuroraEmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Open schedule for $selectedDateIso",
                        subtitle = "Tap + Add Appointment below to book an estimate, walkthrough, or service job.",
                        actionLabel = "+ Add Appointment",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                items(dayJobs, key = { it.id }) { job ->
                    AuroraCard(
                        highlighted = job.status == JobStatus.IN_PROGRESS.code,
                        onClick = { selectedJob = job },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.width(86.dp)) {
                                Text(
                                    text = job.scheduledTime.ifBlank { "9:00 AM" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AuroraTextPrimary
                                )
                                Text(
                                    text = job.durationLabel.ifBlank { "1h" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTextMuted
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AuroraTextPrimary
                                )
                                Text(
                                    text = job.clientName + if (job.address.isNotBlank()) " • ${job.address}" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (job.status) {
                                    JobStatus.COMPLETED.code -> AuroraEmerald.copy(alpha = 0.2f)
                                    JobStatus.URGENT.code -> AuroraAmber.copy(alpha = 0.2f)
                                    else -> AuroraCardElevated
                                }
                            ) {
                                Text(
                                    text = JobStatus.fromCode(job.status).label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (job.status == JobStatus.COMPLETED.code) AuroraEmerald else AuroraTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        AuroraPrimaryCta(
            text = "Add Appointment",
            icon = Icons.Outlined.Add,
            testTag = "schedule_add_appointment_button",
            onClick = { showAddDialog = true }
        )
    }

    if (selectedJob != null) {
        val job = selectedJob!!
        AlertDialog(
            onDismissRequest = { selectedJob = null },
            containerColor = AuroraCardElevated,
            title = { Text("${job.title} — ${job.clientName}", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${job.scheduledDate} at ${job.scheduledTime} (${job.durationLabel})", color = AuroraEmerald)
                    if (job.address.isNotBlank()) Text("Location: ${job.address}", color = AuroraTextSecondary)
                    if (job.description.isNotBlank()) Text(job.description, color = AuroraTextPrimary)
                    AuroraDivider()
                    Text("Job Pack Safety & Steps (${jobPack.serviceType})", style = MaterialTheme.typography.titleSmall, color = AuroraEmerald)
                    jobPack.checklist.take(3).forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
                    }
                    if (job.status == JobStatus.COMPLETED.code) {
                        Text(
                            text = "Job Completed (status locked)",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuroraEmerald
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AuroraFilterPill(
                                label = "In Progress",
                                selected = job.status == JobStatus.IN_PROGRESS.code,
                                testTag = "job_status_inprogress",
                                onClick = {
                                    onUpdateJobStatus(job, JobStatus.IN_PROGRESS)
                                    selectedJob = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AuroraFilterPill(
                                label = "Complete",
                                selected = false,
                                testTag = "job_status_complete",
                                onClick = {
                                    onUpdateJobStatus(job, JobStatus.COMPLETED)
                                    selectedJob = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    TextButton(onClick = {
                        selectedJob = null
                        onCreateInvoiceForJob(job)
                    }) {
                        Text("Create Invoice from Job", color = AuroraEmerald)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedJob = null }) {
                    Text("Done", color = AuroraTextPrimary)
                }
            }
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var clientName by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("9:00 AM") }
        var duration by remember { mutableStateOf("1h") }
        var valueStr by remember { mutableStateOf("250") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Schedule Appointment ($selectedDateIso)", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Service Title (e.g. Estimate)") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Customer Name") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Street Address") }, singleLine = true, colors = auroraFieldColors())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.weight(1f), colors = auroraFieldColors())
                        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, singleLine = true, modifier = Modifier.weight(1f), colors = auroraFieldColors())
                    }
                    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it }, label = { Text("Estimated Value ($)") }, singleLine = true, colors = auroraFieldColors())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && clientName.isNotBlank()) {
                        onAddJob(
                            title, clientName, "", address, selectedDateIso,
                            time, duration, business?.services?.firstOrNull() ?: "Service",
                            valueStr.toDoubleOrNull() ?: 0.0, JobPriority.MEDIUM, ""
                        )
                        showAddDialog = false
                    }
                }) {
                    Text("Schedule", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }
}
