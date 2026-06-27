package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBottomBarHeight = staticCompositionLocalOf { 0.dp }

/**
 * Approximate bottom inset so vertically centered empty/error content sits in the visible
 * area above the mini player and navigation bar (fixed offset, not tied to measured height).
 */
private val BottomBarCenteredContentOffset = 80.dp

@Composable
fun Modifier.centeredScreenContentBottomPadding(): Modifier {
    if (LocalBottomBarHeight.current <= 0.dp) return this
    return padding(bottom = BottomBarCenteredContentOffset)
}

@Composable
fun bottomBarListContentPadding(
    horizontal: Dp = 16.dp,
    top: Dp = 8.dp,
    extraBottom: Dp = 8.dp,
): PaddingValues {
    val bottomBarHeight = LocalBottomBarHeight.current
    return PaddingValues(
        start = horizontal,
        top = top,
        end = horizontal,
        bottom = bottomBarHeight + extraBottom,
    )
}
