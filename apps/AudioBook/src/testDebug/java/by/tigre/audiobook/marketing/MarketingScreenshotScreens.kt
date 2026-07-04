package by.tigre.audiobook.marketing

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.presentation.audiobook_catalog.view.AudiobookChapterListSheet
import by.tigre.media.platform.background.R

@Composable
fun MarketingChapterListScreen(controller: AudiobookPlaybackController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        AudiobookChapterListSheet(controller = controller)
    }
}

@Composable
fun MarketingWidgetsHomeScreen(
    context: Context,
    locale: MarketingScreenshotLocale,
) {
    val bookTitle = MarketingScreenshotResources.string(locale, "screenshot_book_war_peace")
    val chapterTitle = MarketingScreenshotResources.string(locale, "screenshot_chapter_title", 12)
    val coverFile = MarketingScreenshotResources.coverUri(
        context,
        locale,
        if (locale == MarketingScreenshotLocale.Ru) "war_peace.png" else "war_peace.jpg",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B3D52),
                        Color(0xFF1A2838),
                        Color(0xFF121A24),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MarketingWidgetPreview(
                context = context,
                layoutRes = R.layout.widget_playback_large,
                title = bookTitle,
                subtitle = chapterTitle,
                coverFilePath = coverFile.absolutePath,
                height = 148.dp,
            )
            MarketingWidgetPreview(
                context = context,
                layoutRes = R.layout.widget_playback_medium,
                title = bookTitle,
                subtitle = null,
                coverFilePath = null,
                height = 120.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarketingWidgetPreview(
                    context = context,
                    layoutRes = R.layout.widget_playback_wide,
                    title = bookTitle,
                    subtitle = null,
                    coverFilePath = null,
                    height = 80.dp,
                    modifier = Modifier.weight(1f),
                )
                MarketingWidgetPreview(
                    context = context,
                    layoutRes = R.layout.widget_playback_compact,
                    title = bookTitle,
                    subtitle = null,
                    coverFilePath = null,
                    height = 80.dp,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            LauncherDock()
        }
    }
}

@Composable
private fun LauncherDock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
            )
        }
    }
}

@Composable
private fun MarketingWidgetPreview(
    context: Context,
    layoutRes: Int,
    title: String,
    subtitle: String?,
    coverFilePath: String?,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        factory = { viewContext ->
            val root = LayoutInflater.from(viewContext).inflate(layoutRes, null, false)
            bindWidgetPreview(
                root = root,
                title = title,
                subtitle = subtitle,
                coverFilePath = coverFilePath,
            )
            root
        },
    )
}

private fun bindWidgetPreview(
    root: View,
    title: String,
    subtitle: String?,
    coverFilePath: String?,
) {
    root.findViewById<TextView>(R.id.widget_title)?.text = title
    subtitle?.let { value ->
        root.findViewById<TextView>(R.id.widget_subtitle)?.apply {
            text = value
            visibility = View.VISIBLE
        }
    }
    root.findViewById<ImageView>(R.id.widget_play_pause)?.setImageResource(android.R.drawable.ic_media_pause)
    root.findViewById<ImageView>(R.id.widget_skip_prev)?.setImageResource(android.R.drawable.ic_media_previous)
    root.findViewById<ImageView>(R.id.widget_skip_next)?.setImageResource(android.R.drawable.ic_media_next)
    coverFilePath?.let { path ->
        val bitmap = BitmapFactory.decodeFile(path)
        root.findViewById<ImageView>(R.id.widget_cover)?.setImageBitmap(bitmap)
    }
}
