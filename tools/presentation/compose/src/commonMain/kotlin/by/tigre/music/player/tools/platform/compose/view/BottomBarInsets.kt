package by.tigre.music.player.tools.platform.compose.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBottomBarHeight = staticCompositionLocalOf { 0.dp }

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
