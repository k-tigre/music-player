package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult

data class ArtistListScreenData(
    val searchQuery: String,
    val artists: List<Artist>,
    val searchResult: CatalogSearchResult?,
)
