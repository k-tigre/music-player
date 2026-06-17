package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

interface ArtistListComponent {

    val screenState: StateFlow<ScreenContentState<ArtistListScreenData>>
    val searchQuery: StateFlow<String>

    fun retry()
    fun onSearchQueryChanged(query: String)
    fun onArtistClicked(artist: Artist)
    fun onAddToPlayArtistClicked(artist: Artist)
    fun onPlayArtistClicked(artist: Artist)
    fun onSearchSongClicked(song: Song)
    fun onPlaySearchSongClicked(song: Song)
    fun onAddSearchSongClicked(song: Song)

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator
    ) : ArtistListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val playbackController: PlaybackController = dependency.playbackController

        private val _searchQuery = MutableStateFlow("")
        override val searchQuery: StateFlow<String> = _searchQuery

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                combine(
                    catalogSource.dataVersion,
                    _searchQuery
                ) { _, query -> query }
                    .flatMapLatest { query ->
                        flow {
                            if (query.isBlank()) {
                                emit(
                                    ArtistListScreenData(
                                        searchQuery = query,
                                        artists = catalogSource.getArtists(),
                                        searchResult = null
                                    )
                                )
                            } else {
                                emit(
                                    ArtistListScreenData(
                                        searchQuery = query,
                                        artists = emptyList(),
                                        searchResult = catalogSource.search(query)
                                    )
                                )
                            }
                        }
                    }
            },
            mapDataToState = { data ->
                ScreenContentState.Content(data)
            }
        )

        override val screenState: StateFlow<ScreenContentState<ArtistListScreenData>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onSearchQueryChanged(query: String) {
            _searchQuery.update { query }
        }

        override fun onArtistClicked(artist: Artist) {
            navigator.showShowAlbums(artist)
        }

        override fun onAddToPlayArtistClicked(artist: Artist) {
            playbackController.addArtistToPlay(artist.id)
        }

        override fun onPlayArtistClicked(artist: Artist) {
            playbackController.playArtist(artist.id)
        }

        override fun onSearchSongClicked(song: Song) {
            navigator.showSongsForTrack(song)
        }

        override fun onPlaySearchSongClicked(song: Song) {
            playbackController.playSong(song.id)
        }

        override fun onAddSearchSongClicked(song: Song) {
            playbackController.addSongToPlay(song.id)
        }
    }
}
