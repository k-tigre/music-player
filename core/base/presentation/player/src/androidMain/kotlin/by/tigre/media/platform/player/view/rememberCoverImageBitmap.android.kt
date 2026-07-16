package by.tigre.media.platform.player.view

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

@Composable
actual fun rememberCoverImageBitmap(coverModel: Any?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(coverModel) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(coverModel) {
        bitmap = if (coverModel == null) {
            null
        } else {
            try {
                val loader = ImageLoader(context)
                val result = loader.execute(
                    ImageRequest.Builder(context)
                        .data(coverModel)
                        .allowHardware(false)
                        .size(256)
                        .build(),
                )
                when (val image = result.image) {
                    null -> null
                    is BitmapDrawable -> image.bitmap.asImageBitmap()
                    else -> image.toBitmap().asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    return bitmap
}
