package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun CoverThumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    cornerRadius: Dp = 8.dp,
    fallbackIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val appIcon = rememberAppIconPainter()
    val placeholder: Painter = if (fallbackIcon != null) {
        rememberVectorPainter(fallbackIcon)
    } else {
        appIcon
    }

    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
}
