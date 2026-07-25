package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.runtime.Composable

@Composable
actual fun rememberResolvedCoverModel(model: Any?): Any? = model

actual fun logCoverLoadError(model: Any?, error: Throwable) = Unit
