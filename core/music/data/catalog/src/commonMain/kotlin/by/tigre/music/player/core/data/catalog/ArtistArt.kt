package by.tigre.music.player.core.data.catalog

sealed interface ArtistArt {
    data class Ready(val model: ArtistArtModel) : ArtistArt
    data object Loading : ArtistArt
    data object Missing : ArtistArt
    data object Idle : ArtistArt
}

sealed interface ArtistArtModel {
    data class File(val path: String) : ArtistArtModel
    data class Url(val url: String) : ArtistArtModel
}
