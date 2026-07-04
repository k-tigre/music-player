package by.tigre.audiobook.marketing

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.asImage
import coil3.toBitmap
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

@OptIn(ExperimentalCoilApi::class)
@Composable
fun MarketingScreenshotCoilScope(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val previewHandler = remember(context) { marketingScreenshotCoilPreviewHandler(context) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        content()
    }
}

@OptIn(ExperimentalCoilApi::class)
fun marketingScreenshotCoilPreviewHandler(@Suppress("UNUSED_PARAMETER") context: Context): AsyncImagePreviewHandler =
    AsyncImagePreviewHandler { imageLoader, request ->
        decodeMarketingCoverFile(request)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                val success = SuccessResult(
                    image = bitmap.asImage(),
                    request = request,
                    dataSource = coil3.decode.DataSource.MEMORY,
                )
                return@AsyncImagePreviewHandler AsyncImagePainter.State.Success(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    result = success,
                )
            }
        }
        when (val result = imageLoader.execute(request)) {
            is SuccessResult -> AsyncImagePainter.State.Success(
                painter = BitmapPainter(result.image.toBitmap().asImageBitmap()),
                result = result,
            )
            is ErrorResult -> AsyncImagePainter.State.Error(
                painter = null,
                result = result,
            )
        }
    }

private fun decodeMarketingCoverFile(request: ImageRequest): File? =
    request.data.toMarketingCoverFile()

private fun Any?.toMarketingCoverFile(): File? = when (this) {
    is File -> this
    is Uri -> this.path?.let(::File)
    is String -> when {
        startsWith("file:") -> Uri.parse(this).path?.let(::File)
        else -> File(this)
    }
    else -> null
}
