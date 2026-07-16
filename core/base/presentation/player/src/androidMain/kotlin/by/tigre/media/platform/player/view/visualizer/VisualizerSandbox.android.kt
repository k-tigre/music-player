package by.tigre.media.platform.player.view.visualizer

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberVisualizerSandboxEnabled(): Boolean {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
