package by.tigre.debug_settings

import by.tigre.media.platform.tools.platform.compose.ComposableView

interface DebugPageComponent {
    val title: String
}

interface ComposableDebugPage : DebugPageComponent {
    val view: ComposableView
}
