package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Use on [androidx.compose.material3.NavigationBar] inside [BottomBarContainer] to avoid double inset padding. */
val BottomBarNavigationBarInsets = WindowInsets(0, 0, 0, 0)

@Composable
fun BottomBarContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding(),
            content = content,
        )
    }
}
