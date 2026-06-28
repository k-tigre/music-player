package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Use on [androidx.compose.material3.NavigationBar] inside [BottomBarContainer] to avoid double inset padding. */
val BottomBarNavigationBarInsets = WindowInsets(0, 0, 0, 0)

@Composable
fun BottomBarContainer(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    applyNavigationBarsPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (containerColor != null) {
                    Modifier.background(containerColor)
                } else {
                    Modifier
                }
            )
            .then(
                if (applyNavigationBarsPadding) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier
                }
            ),
        content = content,
    )
}
