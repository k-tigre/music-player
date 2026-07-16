package by.tigre.media.platform.player.view.visualizer

import androidx.compose.runtime.Composable

/** True on debuggable app builds (debug/qa). Release is always false. */
@Composable
expect fun rememberVisualizerSandboxEnabled(): Boolean
