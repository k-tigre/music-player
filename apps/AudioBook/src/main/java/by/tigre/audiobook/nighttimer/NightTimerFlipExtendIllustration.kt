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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
        FlipArrow(color = outline)
        FlipStep(
            label = stringResource(R.string.night_timer_flip_step_flipped),
            content = {
                PhoneFlipCanvas(
                    pose = PhonePose.Flipped,
                    outlineColor = outline,
                    screenColor = screen,
                    surfaceColor = surface,
                )
            },
        )
        FlipArrow(color = outline)
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
        modifier = Modifier.width(88.dp),
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

@Composable
private fun FlipArrow(
    color: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 40.dp)) {
        val stroke = Stroke(width = 2.5f, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.55f)
            quadraticBezierTo(
                size.width * 0.5f,
                size.height * 0.05f,
                size.width * 0.9f,
                size.height * 0.45f,
            )
        }
        drawPath(path, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(size.width * 0.9f, size.height * 0.45f),
            end = Offset(size.width * 0.62f, size.height * 0.38f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.9f, size.height * 0.45f),
            end = Offset(size.width * 0.82f, size.height * 0.68f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

private enum class PhonePose {
    Resting,
    Flipped,
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
    Canvas(modifier = Modifier.size(width = 72.dp, height = 88.dp)) {
        val bedY = size.height * 0.78f
        drawLine(
            color = surfaceColor,
            start = Offset(size.width * 0.05f, bedY),
            end = Offset(size.width * 0.95f, bedY),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = surfaceColor.copy(alpha = 0.35f),
            start = Offset(size.width * 0.12f, bedY + 5f),
            end = Offset(size.width * 0.88f, bedY + 5f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        when (pose) {
            PhonePose.Resting -> drawPhoneResting(outlineColor, screenColor, bedY)
            PhonePose.Flipped -> drawPhoneFlipped(outlineColor, screenColor, bedY)
        }

        if (showPlusBadge) {
            drawPlusBadge(badgeColor)
        }
    }
}

private fun DrawScope.drawPhoneResting(
    outlineColor: androidx.compose.ui.graphics.Color,
    screenColor: androidx.compose.ui.graphics.Color,
    bedY: Float,
) {
    val phoneWidth = size.width * 0.42f
    val phoneHeight = size.height * 0.34f
    val left = (size.width - phoneWidth) / 2f
    val top = bedY - phoneHeight

    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(left, top),
        size = Size(phoneWidth, phoneHeight),
        cornerRadius = CornerRadius(phoneWidth * 0.12f, phoneWidth * 0.12f),
        style = Stroke(width = 2.5f),
    )
    drawRoundRect(
        color = screenColor.copy(alpha = 0.25f),
        topLeft = Offset(left + phoneWidth * 0.12f, top + phoneHeight - phoneHeight * 0.22f),
        size = Size(phoneWidth * 0.76f, phoneHeight * 0.14f),
        cornerRadius = CornerRadius(4f, 4f),
    )
    drawLine(
        color = outlineColor.copy(alpha = 0.5f),
        start = Offset(left + phoneWidth * 0.35f, top + phoneHeight * 0.18f),
        end = Offset(left + phoneWidth * 0.65f, top + phoneHeight * 0.18f),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPhoneFlipped(
    outlineColor: androidx.compose.ui.graphics.Color,
    screenColor: androidx.compose.ui.graphics.Color,
    bedY: Float,
) {
    val phoneWidth = size.width * 0.42f
    val phoneHeight = size.height * 0.34f
    val pivot = Offset(size.width / 2f, bedY)

    rotate(degrees = 180f, pivot = pivot) {
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(pivot.x - phoneWidth / 2f, pivot.y - phoneHeight),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(phoneWidth * 0.12f, phoneWidth * 0.12f),
            style = Stroke(width = 2.5f),
        )
        val screenTop = pivot.y - phoneHeight + phoneHeight * 0.12f
        drawRoundRect(
            color = screenColor.copy(alpha = 0.55f),
            topLeft = Offset(pivot.x - phoneWidth * 0.38f, screenTop),
            size = Size(phoneWidth * 0.76f, phoneHeight * 0.62f),
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}

private fun DrawScope.drawPlusBadge(
    badgeColor: androidx.compose.ui.graphics.Color,
) {
    val badgeSize = size.width * 0.34f
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
