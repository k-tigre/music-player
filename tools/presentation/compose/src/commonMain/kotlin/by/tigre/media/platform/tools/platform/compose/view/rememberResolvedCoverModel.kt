package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.runtime.Composable

@Composable
expect fun rememberResolvedCoverModel(model: Any?): Any?

expect fun logCoverLoadError(model: Any?, error: Throwable)
