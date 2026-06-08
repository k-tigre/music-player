package by.tigre.music.player.tools.analytics.music

import by.tigre.music.player.tools.analytics.common.AnalyticsAction
import by.tigre.music.player.tools.analytics.common.AnalyticsApp
import by.tigre.music.player.tools.analytics.common.AnalyticsDoc
import by.tigre.music.player.tools.analytics.common.AnalyticsScope
import by.tigre.music.player.tools.analytics.common.AnalyticsScreen
import by.tigre.music.player.tools.analytics.common.WithPayload

object MusicEvents {

    sealed class Action(override val name: String) : AnalyticsAction {

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open playback queue")
        data object NavOpenQueue : Action("music_nav_open_queue")

        @AnalyticsScope(AnalyticsApp.PLAYER, AnalyticsApp.DESKTOP)
        @AnalyticsDoc("Open music catalog tab")
        data object NavOpenCatalog : Action("music_nav_open_catalog")

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
