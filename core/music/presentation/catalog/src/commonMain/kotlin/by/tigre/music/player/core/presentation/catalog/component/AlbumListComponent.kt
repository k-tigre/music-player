package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.AddToPlaylistRequest
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import `by`.tigre.music.player.core.presentation.catalog.resources.Res
import `by`.tigre.music.player.core.presentation.catalog.resources.*
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

interface AlbumListComponent {

    val screenState: StateFlow<ScreenContentState<List<Album>>>
    val artist: Artist
    val removePrompt: StateFlow<RemovePrompt?>

    fun retry()
    fun onAlbumClicked(album: Album)
    fun onBackClicked()
    fun onPlayAlbumClicked(album: Album)
    fun onAddToPlayAlbumClicked(album: Album)
    fun onAddAlbumToPlaylistClicked(album: Album)
    fun onRemoveAlbumClicked(album: Album)
    fun confirmHide()
    fun confirmDeleteForever()
    fun dismissRemove()
    fun onToggleAlbumFavorite(album: Album)

    val favoriteIds: StateFlow<Set<Song.Id>>
    val likedAlbumIds: StateFlow<Set<Album.Id>>

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
        override val artist: Artist
    ) : AlbumListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val playbackController: PlaybackController = dependency.playbackController
        private val addToPlaylistCoordinator: AddToPlaylistCoordinator = dependency.addToPlaylistCoordinator
        private val favoritesRepository: FavoritesRepository = dependency.favoritesRepository
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        override val favoriteIds: StateFlow<Set<Song.Id>> = favoritesRepository.favoriteIds
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

        override val likedAlbumIds: StateFlow<Set<Album.Id>> = favoritesRepository.likedAlbums
            .map { albums ->
                albums
                    .filter { it.artistId == artist.id }
                    .map { it.album.id }
                    .toSet()
            }
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

        private val _removePrompt = MutableStateFlow<RemovePrompt?>(null)
        override val removePrompt: StateFlow<RemovePrompt?> = _removePrompt

        private var pendingAlbum: Album? = null

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                catalogSource.dataVersion.flatMapLatest {
                    flow { emit(catalogSource.getAlbums(artist.id)) }
                }
            },
            mapDataToState = { albums ->
                Content(albums)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Album>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onBackClicked() {
            navigator.showPreviousScreen()
        }

        override fun onAlbumClicked(album: Album) {
            navigator.showSongs(album, artist)
        }

        override fun onPlayAlbumClicked(album: Album) {
            eventAnalytics.trackEvent(MusicEvents.Action.CatalogPlayAlbum)
            playbackController.playAlbum(album.id, artist.id)
        }

        override fun onAddToPlayAlbumClicked(album: Album) {
            eventAnalytics.trackEvent(MusicEvents.Action.CatalogAddAlbumToQueue)
            playbackController.addAlbumToPlay(album.id, artist.id)
        }

        override fun onAddAlbumToPlaylistClicked(album: Album) {
            launch {
                val songIds = catalogSource.getSongsByAlbum(
                    artistId = artist.id,
                    albumId = album.id
                ).map(Song::id)
                if (songIds.isEmpty()) return@launch
                addToPlaylistCoordinator.show(
                    AddToPlaylistRequest(
                        songIds = songIds,
                        previewText = getString(
                            Res.string.catalog_add_tracks_from_album_preview,
                            songIds.size,
                            album.name
                        )
                    )
                )
            }
        }

        override fun onRemoveAlbumClicked(album: Album) {
            pendingAlbum = album
            _removePrompt.value = RemovePrompt(
                onHide = ::confirmHide,
                onDeleteForever = ::confirmDeleteForever
            )
        }

        override fun confirmHide() {
            val album = pendingAlbum ?: return
            launch {
                val songIds = catalogSource.hideAlbum(artist.id, album.id)
                playbackController.removeSongsFromQueue(songIds)
                clearRemovePrompt()
                stateDelegate.reload()
            }
        }

        override fun confirmDeleteForever() {
            val album = pendingAlbum ?: return
            launch {
                val songIds = catalogSource.deleteAlbum(artist.id, album.id)
                if (songIds.isNotEmpty()) {
                    playbackController.removeSongsFromQueue(songIds)
                    stateDelegate.reload()
                }
                clearRemovePrompt()
            }
        }

        override fun dismissRemove() {
            clearRemovePrompt()
        }

        override fun onToggleAlbumFavorite(album: Album) {
            launch {
                val isFavorite = favoritesRepository.toggleAlbum(artist.id, album.id)
                eventAnalytics.trackEvent(MusicEvents.Action.FavoriteToggle(isFavorite))
            }
        }

        private fun clearRemovePrompt() {
            pendingAlbum = null
            _removePrompt.value = null
        }
    }
}
