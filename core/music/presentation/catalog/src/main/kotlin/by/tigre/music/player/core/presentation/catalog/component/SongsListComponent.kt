package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.presentation.base.ScreenContentState.Content
import by.tigre.music.player.presentation.base.ScreenContentStateDelegate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

interface SongsListComponent {

    val screenState: StateFlow<ScreenContentState<List<Song>>>
    val album: Album

    fun retry()
    fun onPlaySongClicked(song: Song)
    fun onAddSongClicked(song: Song)
    fun onBackClicked()

    class Impl(
        context: BaseComponentContext,
        dependency: CatalogDependency,
        private val navigator: CatalogNavigator,
        override val album: Album,
        artist: Artist
    ) : SongsListComponent, BaseComponentContext by context {

        private val catalogSource: CatalogSource = dependency.catalogSource
        private val playbackController: PlaybackController = dependency.playbackController

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                flow { emit(catalogSource.getSongsByAlbum(artistId = artist.id, albumId = album.id)) }
            },
            mapDataToState = { songs ->
                Content(songs)
            }
        )

        override val screenState: StateFlow<ScreenContentState<List<Song>>> = stateDelegate.screenState

        override fun retry() {
            stateDelegate.reload()
        }

        override fun onPlaySongClicked(song: Song) {
            playbackController.playSong(song.id)
        }

        override fun onAddSongClicked(song: Song) {
            playbackController.addSongToPlay(song.id)
        }

        override fun onBackClicked() {
            navigator.showPreviousScreen()
        }
    }
}
