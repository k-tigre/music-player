package by.tigre.music.player.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import by.tigre.music.player.core.data.catalog.android.ExternalAudioUriResolver
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.di.ApplicationGraph
import by.tigre.music.player.core.entiry.playback.PlayableItem
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ExternalAudioIntentHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val graph: ApplicationGraph,
) {

    fun handle(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return

        takePersistablePermission(intent, uri)

        scope.launch {
            runCatching { handleUri(uri) }.onFailure { error ->
                if (error is SecurityException) {
                    Toast.makeText(
                        context,
                        context.getString(by.tigre.music.player.R.string.external_audio_access_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private suspend fun handleUri(uri: Uri) {
        val catalog = graph.catalogSource
        val playbackController: PlaybackController = graph.playbackController
        val analyticsSource = ExternalAudioUriResolver.resolveAnalyticsSource(uri)

        val catalogSongId = ExternalAudioUriResolver.resolveSongId(context, uri, catalog)
        if (catalogSongId != null) {
            graph.eventAnalytics.trackEvent(
                MusicEvents.Action.ExternalAudioOpened(
                    source = analyticsSource,
                    resolvedToCatalog = true,
                )
            )
            playbackController.playSong(catalogSongId)
            return
        }

        val title = ExternalAudioUriResolver.resolveDisplayTitle(context, uri)
        val sourceLabel = when (ExternalAudioUriResolver.resolveSourceLabel(uri)) {
            ExternalAudioUriResolver.SOURCE_DOWNLOADS ->
                context.getString(by.tigre.music.player.R.string.external_audio_source_downloads)
            else -> null
        }

        graph.eventAnalytics.trackEvent(
            MusicEvents.Action.ExternalAudioOpened(
                source = analyticsSource,
                resolvedToCatalog = false,
            )
        )
        playbackController.playExternal(
            PlayableItem.ExternalAudio(
                uri = uri.toString(),
                title = title,
                sourceLabel = sourceLabel,
            )
        )
    }

    private fun takePersistablePermission(intent: Intent, uri: Uri) {
        val flags = intent.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        if (flags != 0) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
        }
    }
}
