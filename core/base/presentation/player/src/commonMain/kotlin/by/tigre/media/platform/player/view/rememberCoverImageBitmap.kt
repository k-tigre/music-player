package by.tigre.media.platform.player.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Cover pixels for per-bar color sampling (SquircleBurst). */
@Composable
expect fun rememberCoverImageBitmap(coverModel: Any?): ImageBitmap?
