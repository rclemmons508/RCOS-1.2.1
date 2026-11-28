package com.rcsolutions.rcosmobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorderGlow
import com.rcsolutions.rcosmobile.ui.theme.AuroraCanvasDeep
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmeraldBright
import com.rcsolutions.rcosmobile.ui.theme.AuroraObsidian
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AuroraCanvasDeep,
                        AuroraObsidian,
                        AuroraObsidian
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuroraEmerald.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.08f),
                    radius = size.width * 0.85f
                )
            )
        }
        content()
    }
}

@Composable
fun AuroraCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (highlighted) AuroraBorderGlow.copy(alpha = 0.55f) else AuroraBorder
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = if (highlighted) AuroraCardElevated else AuroraCardSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun AuroraPrimaryCta(
    text: String,
    icon: ImageVector? = null,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuroraEmerald,
            contentColor = AuroraOnEmerald,
            disabledContainerColor = AuroraBorder,
            disabledContentColor = AuroraTextMuted
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun AuroraSecondaryCta(
    text: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Phase 2 (dialpad toggle) — optional leading icon, same pattern
    // as AuroraPrimaryCta. Existing call sites pass nothing and are
    // unchanged.
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AuroraBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AuroraCardSurface,
            contentColor = AuroraTextPrimary
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun AuroraFilterPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) AuroraEmerald else AuroraCardSurface,
        border = BorderStroke(1.dp, if (selected) AuroraEmerald else AuroraBorder)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) AuroraOnEmerald else AuroraTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RcSolutionsBrandLockup(
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bold geometric RC emblem
            Text(
                text = "RC",
                fontSize = if (compact) 26.sp else 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                color = AuroraEmerald
            )
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(if (compact) 24.dp else 38.dp)
                    .background(AuroraTextSecondary.copy(alpha = 0.55f))
            )
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            Column {
                Text(
                    text = "RC",
                    fontSize = if (compact) 11.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = AuroraTextPrimary,
                    lineHeight = if (compact) 12.sp else 18.sp
                )
                Text(
                    text = "SOLUTIONS",
                    fontSize = if (compact) 11.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = AuroraTextPrimary,
                    lineHeight = if (compact) 12.sp else 18.sp
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "PEOPLE • PURPOSE • PROPERTY • PROGRESS",
            fontSize = if (compact) 6.5.sp else 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = if (compact) 1.1.sp else 1.8.sp,
            color = AuroraTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AcousticWaveOrb(
    isActive: Boolean,
    size: Dp = 156.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 1800 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer ambient aurora glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuroraEmerald.copy(alpha = (if (isActive) 0.42f else 0.25f) * pulse),
                        AuroraEmerald.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.12f
                ),
                radius = radius * 1.12f,
                center = center
            )

            // Dark obsidian inner glass sphere
            val orbRadius = radius * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A2416),
                        Color(0xFF030B06)
                    ),
                    center = Offset(center.x, center.y - orbRadius * 0.2f),
                    radius = orbRadius * 1.2f
                ),
                radius = orbRadius,
                center = center
            )

            // Outer glowing ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AuroraEmerald.copy(alpha = 0.2f),
                        AuroraEmeraldBright.copy(alpha = 0.9f),
                        AuroraEmerald.copy(alpha = 0.2f),
                        AuroraEmeraldBright.copy(alpha = 0.7f),
                        AuroraEmerald.copy(alpha = 0.2f)
                    ),
                    center = center
                ),
                radius = orbRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Luminous dual sine waves clipped logically inside sphere horizontal span
            val startX = center.x - orbRadius * 0.88f
            val endX = center.x + orbRadius * 0.88f
            val waveWidth = endX - startX
            val steps = 48

            val path1 = Path()
            val path2 = Path()
            for (i in 0..steps) {
                val t = i.toFloat() / steps.toFloat()
                val x = startX + waveWidth * t
                val envelope = sin(t * PI).toFloat()
                val amp1 = orbRadius * (if (isActive) 0.34f else 0.22f) * envelope
                val amp2 = orbRadius * (if (isActive) 0.24f else 0.16f) * envelope
                val y1 = center.y + sin(t * 2f * PI.toFloat() + phase) * amp1
                val y2 = center.y + cos(t * 2.2f * PI.toFloat() - phase) * amp2
                if (i == 0) {
                    path1.moveTo(x, y1)
                    path2.moveTo(x, y2)
                } else {
                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }
            }

            drawPath(
                path = path2,
                color = AuroraEmerald.copy(alpha = 0.45f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path1,
                color = AuroraEmeraldBright,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun AuroraSectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AuroraTextPrimary
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = AuroraTextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AuroraEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AuroraCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AuroraEmerald.copy(alpha = 0.14f))
                    .border(1.dp, AuroraEmerald.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AuroraEmerald)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(14.dp))
                AuroraPrimaryCta(
                    text = actionLabel,
                    testTag = "empty_state_cta",
                    onClick = onActionClick
                )
            }
        }
    }
}

@Composable
fun AuroraDivider() {
    HorizontalDivider(color = AuroraBorder.copy(alpha = 0.6f), thickness = 1.dp)
}
