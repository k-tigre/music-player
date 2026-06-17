package by.tigre.media.platform.tools.platform.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ComposableView {
    @Composable
    fun Draw(modifier: Modifier)
}
