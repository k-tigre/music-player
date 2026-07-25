package by.tigre.media.platform.tools.platform.compose.view

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import by.tigre.media.platform.tools.platform.utils.CoverArtCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberResolvedCoverModel(model: Any?): Any? {
    val context = LocalContext.current.applicationContext
    return produceState(initialValue = model, model) {
        value = withContext(Dispatchers.IO) {
            CoverArtCache.resolveForDisplay(context, model)
        }
    }.value
}

actual fun logCoverLoadError(model: Any?, error: Throwable) {
    Log.w("CoverArt", "Coil failed model=$model: ${error.message}", error)
}
