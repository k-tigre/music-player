package by.tigre.media.platform.tools.analytics.music

import by.tigre.media.platform.tools.analytics.common.AnalyticsAction
import by.tigre.media.platform.tools.analytics.common.AnalyticsApp
import by.tigre.media.platform.tools.analytics.common.AnalyticsDoc
import by.tigre.media.platform.tools.analytics.common.AnalyticsScope
import by.tigre.media.platform.tools.analytics.common.AnalyticsScreen
import by.tigre.media.platform.tools.analytics.common.WithPayload

object MusicEvents {

    sealed class Action(override val name: String) : AnalyticsAction {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open playback queue")
        data object NavOpenQueue : Action("music_nav_open_queue")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open music catalog tab")
        data object NavOpenCatalog : Action("music_nav_open_catalog")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Catalog search performed after debounce")
        data class CatalogSearch(
            private val queryLengthBucket: QueryLengthBucket,
            private val artistResultCount: Int,
            private val songResultCount: Int,
        ) : Action("music_catalog_search"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "query_length_bucket" to queryLengthBucket.analyticsValue,
                "artist_result_count" to artistResultCount.toString(),
                "song_result_count" to songResultCount.toString(),
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Play song from album song list")
        data object CatalogPlaySong : Action("music_catalog_play_song")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Add song to queue from album song list")
        data object CatalogAddSongToQueue : Action("music_catalog_add_song_to_queue")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Play entire album from album list")
        data object CatalogPlayAlbum : Action("music_catalog_play_album")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Add entire album to queue from album list")
        data object CatalogAddAlbumToQueue : Action("music_catalog_add_album_to_queue")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Select song in current queue")
        data object QueueSongSelected : Action("music_queue_song_selected")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open artist from queue item")
        data object QueueOpenArtist : Action("music_queue_open_artist")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open album from queue item")
        data object QueueOpenAlbum : Action("music_queue_open_album")

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("External audio file opened from another app")
        data class ExternalAudioOpened(
            private val source: String,
            private val resolvedToCatalog: Boolean,
        ) : Action("music_external_audio_opened"), WithPayload {
            override val payload: Map<String, String> = mapOf(
                "source" to source,
                "resolved_to_catalog" to resolvedToCatalog.toString(),
            )
        }

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("External audio overlay ended")
        data class ExternalAudioOverlayEnded(
            private val reason: OverlayEndReason,
        ) : Action("music_external_audio_overlay_ended"), WithPayload {
            override val payload: Map<String, String> = mapOf("reason" to reason.analyticsValue)
        }

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("Default player onboarding prompt shown")
        data object DefaultPlayerPromptShown : Action("music_default_player_prompt_shown")

        @AnalyticsScope(AnalyticsApp.PLAYER)
        @AnalyticsDoc("Default player onboarding prompt action clicked")
        data object DefaultPlayerPromptClicked : Action("music_default_player_prompt_clicked")
    }

    enum class QueryLengthBucket(val analyticsValue: String) {
        Short("1_2"),
        Medium("3_5"),
        Long("6_10"),
        VeryLong("11_plus"),
        ;

        companion object {
            fun fromQueryLength(length: Int): QueryLengthBucket = when {
                length <= 2 -> Short
                length <= 5 -> Medium
                length <= 10 -> Long
                else -> VeryLong
            }
        }
    }

    enum class OverlayEndReason(val analyticsValue: String) {
        ReturnButton("return_button"),
        Next("next"),
        Ended("ended"),
        PlayCatalog("play_catalog"),
    }

    sealed class Screen(
        override val name: String,
        override val skip: Boolean = false,
    ) : AnalyticsScreen {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Playback queue tab")
        data object Queue : Screen("music_screen_queue")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Music catalog tab")
        data object CatalogTab : Screen("music_screen_catalog_tab")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Artists list in music catalog")
        data object ArtistsList : Screen("music_screen_artists_list")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Albums list for selected artist")
        data class AlbumsList(private val artistId: Long) : Screen("music_screen_albums_list"), WithPayload {
            override val payload: Map<String, String> by lazy { mapOf("artist_id" to artistId.toString()) }
        }

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Songs list for selected album")
        data class SongsList(
            private val albumId: Long,
            private val artistId: Long,
        ) : Screen("music_screen_songs_list"), WithPayload {
            override val payload: Map<String, String> by lazy {
                mapOf(
                    "album_id" to albumId.toString(),
                    "artist_id" to artistId.toString(),
                )
            }
        }
    }
}
