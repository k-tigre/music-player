package by.tigre.audiobook.nighttimer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R

@Composable
fun NightTimerFlipExtendHelp(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.night_timer_flip_extend_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.night_timer_flip_extend_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NightTimerFlipExtendDiagram(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun NightTimerFlipExtendDiagram(
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val screen = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlipStep(
            label = stringResource(R.string.night_timer_flip_step_rest),
            content = {
                PhoneFlipCanvas(
                    pose = PhonePose.Resting,
                    outlineColor = outline,
                    screenColor = screen,
                    surfaceColor = surface,
                )
            },
        )
        StepArrow(color = outline, direction = StepArrowDirection.Forward)
        FlipStep(
            label = stringResource(R.string.night_timer_flip_step_flipped),
            content = {
                PhoneFlipCanvas(
                    pose = PhonePose.FaceDown,
                    outlineColor = outline,
                    screenColor = screen,
                    surfaceColor = surface,
                )
            },
        )
        StepArrow(color = outline, direction = StepArrowDirection.Back)
        FlipStep(
            label = stringResource(R.string.night_timer_flip_step_done),
            content = {
                PhoneFlipCanvas(
                    pose = PhonePose.Resting,
                    outlineColor = outline,
                    screenColor = screen,
                    surfaceColor = surface,
                    badgeColor = accent,
                    showPlusBadge = true,
                )
            },
        )
    }
}

@Composable
private fun FlipStep(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(96.dp),
    ) {
        content()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private enum class StepArrowDirection {
    Forward,
    Back,
}

@Composable
private fun StepArrow(
    color: androidx.compose.ui.graphics.Color,
    direction: StepArrowDirection,
) {
    Canvas(modifier = Modifier.size(width = 20.dp, height = 40.dp)) {
        val strokeWidth = 2.5f
        val y = size.height * 0.42f
        val left = size.width * 0.12f
        val right = size.width * 0.88f

        when (direction) {
            StepArrowDirection.Forward -> {
                drawLine(color, Offset(left, y), Offset(right, y), strokeWidth, StrokeCap.Round)
                drawLine(
                    color,
                    Offset(right, y),
                    Offset(right - 7f, y - 5f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(right, y),
                    Offset(right - 7f, y + 5f),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }
            StepArrowDirection.Back -> {
                drawLine(color, Offset(right, y), Offset(left, y), strokeWidth, StrokeCap.Round)
                drawLine(
                    color,
                    Offset(left, y),
                    Offset(left + 7f, y - 5f),
                    strokeWidth,
                    StrokeCap.Round,
                )
                drawLine(
                    color,
                    Offset(left, y),
                    Offset(left + 7f, y + 5f),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }
        }
    }
}

private enum class PhonePose {
    Resting,
    FaceDown,
}

@Composable
private fun PhoneFlipCanvas(
    pose: PhonePose,
    outlineColor: androidx.compose.ui.graphics.Color,
    screenColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    badgeColor: androidx.compose.ui.graphics.Color = outlineColor,
    showPlusBadge: Boolean = false,
) {
    Canvas(modifier = Modifier.size(width = 96.dp, height = 72.dp)) {
        val tableY = size.height * 0.72f
        drawTableSurface(surfaceColor, tableY)

        when (pose) {
            PhonePose.Resting -> drawPhoneLyingOnTable(
                outlineColor = outlineColor,
                screenColor = screenColor,
                tableY = tableY,
                faceUp = true,
            )
            PhonePose.FaceDown -> drawPhoneLyingOnTable(
                outlineColor = outlineColor,
                screenColor = screenColor,
                tableY = tableY,
                faceUp = false,
            )
        }

        if (showPlusBadge) {
            drawPlusBadge(badgeColor)
        }
    }
}

private fun DrawScope.drawTableSurface(
    surfaceColor: androidx.compose.ui.graphics.Color,
    tableY: Float,
) {
    drawLine(
        color = surfaceColor,
        start = Offset(size.width * 0.04f, tableY),
        end = Offset(size.width * 0.96f, tableY),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = surfaceColor.copy(alpha = 0.35f),
        start = Offset(size.width * 0.1f, tableY + 5f),
        end = Offset(size.width * 0.9f, tableY + 5f),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

/**
 * Phone lying flat on a table, seen from a slight angle above.
 * Landscape orientation — wider edge parallel to the table front.
 */
private fun DrawScope.drawPhoneLyingOnTable(
    outlineColor: androidx.compose.ui.graphics.Color,
    screenColor: androidx.compose.ui.graphics.Color,
    tableY: Float,
    faceUp: Boolean,
) {
    val phoneWidth = size.width * 0.72f
    val phoneDepth = size.height * 0.28f
    val left = (size.width - phoneWidth) / 2f
    val top = tableY - phoneDepth
    val corner = phoneDepth * 0.22f

    val bodyRect = Rect(
        offset = Offset(left, top),
        size = Size(phoneWidth, phoneDepth),
    )

    drawRoundRect(
        color = if (faceUp) screenColor.copy(alpha = 0.18f) else outlineColor.copy(alpha = 0.1f),
        topLeft = bodyRect.topLeft,
        size = bodyRect.size,
        cornerRadius = CornerRadius(corner, corner),
    )
    drawRoundRect(
        color = outlineColor,
        topLeft = bodyRect.topLeft,
        size = bodyRect.size,
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 2.5f),
    )

    if (faceUp) {
        val screenInset = phoneDepth * 0.14f
        drawRoundRect(
            color = screenColor.copy(alpha = 0.35f),
            topLeft = Offset(left + screenInset, top + screenInset * 0.7f),
            size = Size(phoneWidth - screenInset * 2f, phoneDepth - screenInset * 1.4f),
            cornerRadius = CornerRadius(corner * 0.6f, corner * 0.6f),
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.45f),
            start = Offset(left + phoneWidth * 0.38f, top + phoneDepth * 0.22f),
            end = Offset(left + phoneWidth * 0.62f, top + phoneDepth * 0.22f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    } else {
        drawLine(
            color = outlineColor.copy(alpha = 0.35f),
            start = Offset(left + phoneWidth * 0.2f, top + phoneDepth * 0.35f),
            end = Offset(left + phoneWidth * 0.8f, top + phoneDepth * 0.35f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.35f),
            start = Offset(left + phoneWidth * 0.2f, top + phoneDepth * 0.65f),
            end = Offset(left + phoneWidth * 0.8f, top + phoneDepth * 0.65f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawPlusBadge(
    badgeColor: androidx.compose.ui.graphics.Color,
) {
    val badgeSize = size.width * 0.28f
    val rect = Rect(
        offset = Offset(size.width - badgeSize - 2f, 2f),
        size = Size(badgeSize, badgeSize),
    )
    drawRoundRect(
        color = badgeColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(badgeSize / 2f, badgeSize / 2f),
    )
    val center = rect.center
    val arm = badgeSize * 0.18f
    drawLine(
        color = androidx.compose.ui.graphics.Color.White,
        start = Offset(center.x - arm, center.y),
        end = Offset(center.x + arm, center.y),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = androidx.compose.ui.graphics.Color.White,
        start = Offset(center.x, center.y - arm),
        end = Offset(center.x, center.y + arm),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
    )
}
