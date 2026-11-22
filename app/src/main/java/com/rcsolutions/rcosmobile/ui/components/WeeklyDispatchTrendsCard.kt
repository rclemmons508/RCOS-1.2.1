package com.rcsolutions.rcosmobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.ui.theme.AuroraAmber
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCyan
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil

enum class DispatchTrendChartMode(val label: String) {
    TREND_OVERLAY("Volume + Trend"),
    STATUS_SPLIT("Dispatched vs Done")
}

private data class DailyDispatchBucket(
    val isoDate: String,
    val dayShort: String,
    val totalCount: Int,
    val completedCount: Int,
    val urgentOrActiveCount: Int,
    val estimatedRevenue: Double
)

private data class WeeklyDispatchSummary(
    val weekRangeLabel: String,
    val isCurrentWeek: Boolean,
    val buckets: List<DailyDispatchBucket>,
    val totalDispatches: Int,
    val completedDispatches: Int,
    val urgentDispatches: Int,
    val totalEstimatedValue: Double
) {
    val completionPercent: Int
        get() = if (totalDispatches <= 0) 0 else ((completedDispatches * 100f) / totalDispatches).toInt()
}

@Composable
fun WeeklyDispatchTrendsCard(
    jobsState: UiState<List<Job>>,
    isRefreshing: Boolean,
    syncLabel: String?,
    syncError: String?,
    onRefreshFromFirestore: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    var chartMode by remember { mutableStateOf(DispatchTrendChartMode.TREND_OVERLAY) }

    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val errorMsg = syncError ?: (jobsState as? UiState.Error)?.message
    val isInitialLoading = jobsState is UiState.Loading && jobs.isEmpty()

    val summary = remember(jobs, weekOffset) {
        aggregateWeeklyDispatches(jobs, weekOffset)
    }

    val modelProducer = remember(chartMode) { CartesianChartModelProducer() }
    LaunchedEffect(modelProducer, summary, chartMode) {
        val totals = summary.buckets.map { it.totalCount }
        val completed = summary.buckets.map { it.completedCount }
        val active = summary.buckets.map { it.urgentOrActiveCount }
        modelProducer.runTransaction {
            when (chartMode) {
                DispatchTrendChartMode.TREND_OVERLAY -> {
                    columnSeries { series(totals) }
                    lineSeries { series(completed) }
                }
                DispatchTrendChartMode.STATUS_SPLIT -> {
                    columnSeries {
                        series(active)
                        series(completed)
                    }
                }
            }
        }
    }

    val dayLabels = remember(summary) { summary.buckets.map { it.dayShort } }
    val maxY = remember(summary) {
        summary.buckets.maxOfOrNull { it.totalCount }?.coerceAtLeast(1) ?: 1
    }
    val yStep = remember(maxY) {
        if (maxY <= 5) 1.0 else ceil(maxY / 4.0)
    }

    AuroraCard(
        highlighted = true,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_weekly_trends_card")
    ) {
        // Card Header + Week Navigation + Firestore Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = AuroraEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Weekly Dispatch Trends",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AuroraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = buildString {
                        append(if (summary.isCurrentWeek) "This Week" else "Week of ${summary.weekRangeLabel}")
                        if (!syncLabel.isNullOrBlank()) {
                            append(" • ")
                            append(syncLabel)
                        } else {
                            append(" • Firestore Live")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { weekOffset-- },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("weekly_trends_prev_week_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Previous week",
                        tint = AuroraTextSecondary
                    )
                }
                IconButton(
                    onClick = { if (weekOffset < 0) weekOffset++ },
                    enabled = weekOffset < 0,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("weekly_trends_next_week_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Next week",
                        tint = if (weekOffset < 0) AuroraTextSecondary else AuroraTextMuted.copy(alpha = 0.4f)
                    )
                }
                IconButton(
                    onClick = onRefreshFromFirestore,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("weekly_trends_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = AuroraEmerald,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh weekly dispatch chart",
                            tint = AuroraEmerald
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // KPI Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AuroraCardElevated.copy(alpha = 0.7f))
                .border(1.dp, AuroraBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeeklyKpiMetric(
                label = "Dispatched",
                value = summary.totalDispatches.toString(),
                accentColor = AuroraEmerald,
                modifier = Modifier.weight(1f)
            )
            WeeklyKpiMetric(
                label = "Completed",
                value = "${summary.completedDispatches} (${summary.completionPercent}%)",
                accentColor = AuroraCyan,
                modifier = Modifier.weight(1f)
            )
            WeeklyKpiMetric(
                label = "Urgent",
                value = summary.urgentDispatches.toString(),
                accentColor = if (summary.urgentDispatches > 0) AuroraAmber else AuroraTextSecondary,
                modifier = Modifier.weight(0.8f)
            )
            WeeklyKpiMetric(
                label = "Est. Value",
                value = "$${summary.totalEstimatedValue.toInt()}",
                accentColor = AuroraTextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Chart Mode Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DispatchTrendChartMode.entries.forEach { mode ->
                AuroraFilterPill(
                    label = mode.label,
                    selected = chartMode == mode,
                    testTag = "weekly_trends_mode_${mode.name.lowercase()}",
                    onClick = { chartMode = mode },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (errorMsg != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuroraRed.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AuroraRed.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Firestore chart error",
                        tint = AuroraRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraTextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onRefreshFromFirestore,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("weekly_trends_retry_button")
                    ) {
                        Text("Retry", color = AuroraEmerald)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            isInitialLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AuroraEmerald, strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Loading weekly dispatch chart...",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraTextSecondary
                        )
                    }
                }
            }

            summary.totalDispatches == 0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuroraCardElevated.copy(alpha = 0.45f))
                        .border(1.dp, AuroraBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No dispatches scheduled for ${summary.weekRangeLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = AuroraTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Appointments added in Schedule automatically plot across the 7-day trend chart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onOpenSchedule,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("weekly_trends_open_schedule_button")
                    ) {
                        Text("Open Schedule & Dispatch", color = AuroraEmerald)
                    }
                }
            }

            else -> {
                ProvideVicoTheme(
                    rememberM3VicoTheme(
                        columnCartesianLayerColors = listOf(AuroraEmerald, AuroraCyan),
                        lineCartesianLayerColors = listOf(AuroraAmber),
                        lineColor = AuroraBorder.copy(alpha = 0.7f),
                        textColor = AuroraTextSecondary
                    )
                ) {
                    val xAxisFormatter = remember(dayLabels) {
                        CartesianValueFormatter { _, value, _ ->
                            dayLabels.getOrElse(value.toInt()) { "" }
                        }
                    }
                    val yAxisFormatter = remember {
                        CartesianValueFormatter { _, value, _ ->
                            value.toInt().toString()
                        }
                    }

                    val totalColumn = rememberLineComponent(
                        fill = fill(AuroraEmerald),
                        thickness = 16.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30)
                    )
                    val activeColumn = rememberLineComponent(
                        fill = fill(AuroraAmber),
                        thickness = 12.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30)
                    )
                    val completedColumn = rememberLineComponent(
                        fill = fill(AuroraCyan),
                        thickness = 12.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30)
                    )
                    val completionLine = LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(AuroraAmber)),
                        areaFill = LineCartesianLayer.AreaFill.single(fill(AuroraAmber.copy(alpha = 0.16f)))
                    )

                    key(chartMode) {
                        val chart = if (chartMode == DispatchTrendChartMode.TREND_OVERLAY) {
                            rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(totalColumn)
                                ),
                                rememberLineCartesianLayer(
                                    lineProvider = LineCartesianLayer.LineProvider.series(completionLine)
                                ),
                                startAxis = VerticalAxis.rememberStart(
                                    valueFormatter = yAxisFormatter,
                                    itemPlacer = VerticalAxis.ItemPlacer.step({ yStep })
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = xAxisFormatter
                                )
                            )
                        } else {
                            rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                        activeColumn,
                                        completedColumn
                                    ),
                                    mergeMode = { ColumnCartesianLayer.MergeMode.grouped(columnSpacing = 4.dp) }
                                ),
                                startAxis = VerticalAxis.rememberStart(
                                    valueFormatter = yAxisFormatter,
                                    itemPlacer = VerticalAxis.ItemPlacer.step({ yStep })
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = xAxisFormatter
                                )
                            )
                        }

                        CartesianChartHost(
                            chart = chart,
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .testTag("weekly_dispatch_vico_chart")
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Chart Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chartMode == DispatchTrendChartMode.TREND_OVERLAY) {
                        ChartLegendBadge(color = AuroraEmerald, label = "Dispatched Volume")
                        ChartLegendBadge(color = AuroraAmber, label = "Completed Trend")
                    } else {
                        ChartLegendBadge(color = AuroraAmber, label = "Active / Urgent")
                        ChartLegendBadge(color = AuroraCyan, label = "Completed")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = summary.weekRangeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyKpiMetric(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = AuroraTextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun ChartLegendBadge(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AuroraTextSecondary
        )
    }
}

private fun aggregateWeeklyDispatches(
    jobs: List<Job>,
    weekOffset: Int
): WeeklyDispatchSummary {
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFmt = SimpleDateFormat("EEE", Locale.US)
    val rangeFmt = SimpleDateFormat("MMM d", Locale.US)

    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

    val weekDays = (0..6).map {
        val date = cal.time
        val iso = isoFmt.format(date)
        val dayShort = dayFmt.format(date)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        iso to dayShort
    }

    val startLabel = runCatching {
        val d = isoFmt.parse(weekDays.first().first)
        if (d != null) rangeFmt.format(d) else weekDays.first().first
    }.getOrDefault(weekDays.first().first)

    val endLabel = runCatching {
        val d = isoFmt.parse(weekDays.last().first)
        if (d != null) rangeFmt.format(d) else weekDays.last().first
    }.getOrDefault(weekDays.last().first)

    val dayIsoSet = weekDays.map { it.first }.toSet()
    val groupedByDate = mutableMapOf<String, MutableList<Job>>()

    jobs.forEach { job ->
        val rawDate = job.scheduledDate.trim()
        val resolvedIso = when {
            rawDate.length >= 10 && rawDate.substring(0, 10).matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ->
                rawDate.substring(0, 10)
            job.createdAt != null -> isoFmt.format(job.createdAt.toDate())
            job.updatedAt != null -> isoFmt.format(job.updatedAt.toDate())
            else -> ""
        }
        if (resolvedIso in dayIsoSet) {
            groupedByDate.getOrPut(resolvedIso) { mutableListOf() }.add(job)
        }
    }

    val buckets = weekDays.map { (iso, dayShort) ->
        val dayJobs = groupedByDate[iso].orEmpty()
        val completed = dayJobs.count { it.status == JobStatus.COMPLETED.code }
        val activeOrUrgent = dayJobs.size - completed
        val revenue = dayJobs.sumOf { it.estimatedValue }
        DailyDispatchBucket(
            isoDate = iso,
            dayShort = dayShort,
            totalCount = dayJobs.size,
            completedCount = completed,
            urgentOrActiveCount = activeOrUrgent,
            estimatedRevenue = revenue
        )
    }

    val weekJobs = buckets.flatMap { bucket -> groupedByDate[bucket.isoDate].orEmpty() }
    return WeeklyDispatchSummary(
        weekRangeLabel = "$startLabel – $endLabel",
        isCurrentWeek = weekOffset == 0,
        buckets = buckets,
        totalDispatches = weekJobs.size,
        completedDispatches = weekJobs.count { it.status == JobStatus.COMPLETED.code },
        urgentDispatches = weekJobs.count { it.status == JobStatus.URGENT.code },
        totalEstimatedValue = weekJobs.sumOf { it.estimatedValue }
    )
}
