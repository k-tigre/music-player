package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.AddToPlaylistRequest
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import `by`.tigre.music.player.core.presentation.catalog.resources.Res
import `by`.tigre.music.player.core.presentation.catalog.resources.*
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

interface ArtistListComponent {

    val screenState: StateFlow<ScreenContentState<ArtistListScreenData>>
    val searchQuery: StateFlow<String>
    val settingsAvailable: Boolean

    fun retry()
    fun openSettings()
    fun onSearchQueryChanged(query: String)
    fun onArtistClicked(artist: Artist)
    fun onAddToPlayArtistClicked(artist: Artist)
    fun onAddArtistToPlaylistClicked(artist: Artist)
    fun onPlayArtistClicked(artist: Artist)
    fun onSearchSongClicked(song: Song)
    fun onPlaySearchSongClicked(song: Song)
    fun onAddSearchSongClicked(song: Song)

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
        private val onOpenSettings: (() -> Unit)? = null,
    ) : ArtistListComponent, BaseComponentContext by context {

        override val settingsAvailable: Boolean = onOpenSettings != null

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val playbackController: PlaybackController = dependency.playbackController
        private val addToPlaylistCoordinator: AddToPlaylistCoordinator = dependency.addToPlaylistCoordinator
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        private val _searchQuery = MutableStateFlow("")
        override val searchQuery: StateFlow<String> = _searchQuery

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                combine(
                    catalogSource.dataVersion,
                    _searchQuery
                ) { _, query -> query }
                    .debounce(SEARCH_ANALYTICS_DEBOUNCE_MS)
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
                                val searchResult = catalogSource.search(query)
                                eventAnalytics.trackEvent(
                                    MusicEvents.Action.CatalogSearch(
                                        queryLengthBucket = MusicEvents.QueryLengthBucket.fromQueryLength(
                                            query.trim().length
                                        ),
                                        artistResultCount = searchResult.artists.size,
                                        songResultCount = searchResult.songs.size,
                                    )
                                )
                                emit(
                                    ArtistListScreenData(
                                        searchQuery = query,
                                        artists = emptyList(),
                                        searchResult = searchResult
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

        override fun openSettings() {
            onOpenSettings?.invoke()
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

        override fun onAddArtistToPlaylistClicked(artist: Artist) {
            launch {
                val songIds = catalogSource.getSongsByArtist(artist.id).map(Song::id)
                if (songIds.isEmpty()) return@launch
                addToPlaylistCoordinator.show(
                    AddToPlaylistRequest(
                        songIds = songIds,
                        previewText = getString(
                            Res.string.catalog_add_tracks_from_artist_preview,
                            songIds.size,
                            artist.name
                        )
                    )
                )
            }
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

        private companion object {
            const val SEARCH_ANALYTICS_DEBOUNCE_MS = 250L
        }
    }
}
