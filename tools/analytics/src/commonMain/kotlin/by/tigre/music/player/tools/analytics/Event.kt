package by.tigre.music.player.tools.analytics

sealed class Event(val name: String) {

    sealed class Action(name: String) : Event(name) {

        sealed class UI(name: String) : Action("UI_$name") {

            sealed class Button(name: String) : UI("${name}_clicked") {
                data object Play : Button("Play")
                data object Pause : Button("Pause")
                data object Next : Button("Next")
                data object Prev : Button("Prev")
                data object SeekBack15 : Button("SeekBack15")
                data object SeekForward15 : Button("SeekForward15")
                data object SeekBack60 : Button("SeekBack60")
                data object SeekForward60 : Button("SeekForward60")
                data object ShuffleToggle : Button("ShuffleToggle")
                data object OpenEqualizer : Button("OpenEqualizer")
                data object OpenPlayer : Button("OpenPlayer")
                data object OpenQueue : Button("OpenQueue")
                data object OpenCatalog : Button("OpenCatalog")
                data object PlaySong : Button("PlaySong")
                data object AddSongToQueue : Button("AddSongToQueue")
                data object PlayAlbum : Button("PlayAlbum")
                data object AddAlbumToQueue : Button("AddAlbumToQueue")
                data object QueueSongSelected : Button("QueueSongSelected")
                data object OpenArtistFromQueue : Button("OpenArtistFromQueue")
                data object OpenAlbumFromQueue : Button("OpenAlbumFromQueue")
                data object OpenNightTimer : Button("OpenNightTimer")
                data object SelectBook : Button("SelectBook")
                data object OpenFolderSettings : Button("OpenFolderSettings")
            }
        }
    }

    interface WithPayload {
        val payload: Map<String, String>
    }

    sealed class Screen(name: String, val skip: Boolean = false) : Event(name) {

        data object Queue : Screen("Queue")
        data object CatalogTab : Screen("CatalogTab")
        data object ArtistsList : Screen("ArtistsList")

        data class AlbumsList(private val artistId: Long) : Screen("AlbumsList"), WithPayload {
            override val payload: Map<String, String> by lazy { mapOf("artistId" to artistId.toString()) }
        }

        data class SongsList(private val albumId: Long, private val artistId: Long) : Screen("SongsList"), WithPayload {
            override val payload: Map<String, String> by lazy {
                mapOf("albumId" to albumId.toString(), "artistId" to artistId.toString())
            }
        }

        data object Player : Screen("Player")
        data object Equalizer : Screen("Equalizer")
        data object RootOverlay : Screen("RootOverlay", skip = true)

        data object AudiobookCatalog : Screen("AudiobookCatalog")
        data object FolderSelection : Screen("FolderSelection")
        data object BookList : Screen("BookList")
        data object NightTimerSettings : Screen("NightTimerSettings")
    }
}
