package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.di.CatalogDependency
import by.tigre.music.player.core.presentation.catalog.navigation.CatalogNavigator
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.music.player.tools.analytics.music.MusicEventAnalytics
import by.tigre.music.player.tools.analytics.music.MusicEvents
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.presentation.ScreenContentState.Content
import by.tigre.media.platform.presentation.ScreenContentStateDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

interface SongsListComponent {

    val screenState: StateFlow<ScreenContentState<List<Song>>>
    val album: Album
    val removePrompt: StateFlow<RemovePrompt?>

    fun retry()
    fun onPlaySongClicked(song: Song)
    fun onAddSongClicked(song: Song)
    fun onRemoveSongClicked(song: Song)
    fun confirmHide()
    fun confirmDeleteForever()
    fun dismissRemove()
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
        private val eventAnalytics: MusicEventAnalytics = dependency.eventAnalytics

        private val _removePrompt = MutableStateFlow<RemovePrompt?>(null)
        override val removePrompt: StateFlow<RemovePrompt?> = _removePrompt

        private var pendingSong: Song? = null

        private val stateDelegate = ScreenContentStateDelegate(
            scope = this,
            loadData = {
                catalogSource.dataVersion.flatMapLatest {
                    flow { emit(catalogSource.getSongsByAlbum(artistId = artist.id, albumId = album.id)) }
                }
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
            eventAnalytics.trackEvent(MusicEvents.Action.CatalogPlaySong)
            playbackController.playSong(song.id)
        }

        override fun onAddSongClicked(song: Song) {
            eventAnalytics.trackEvent(MusicEvents.Action.CatalogAddSongToQueue)
            playbackController.addSongToPlay(song.id)
        }

        override fun onRemoveSongClicked(song: Song) {
            pendingSong = song
            _removePrompt.value = RemovePrompt(
                onHide = ::confirmHide,
                onDeleteForever = ::confirmDeleteForever
            )
        }

        override fun confirmHide() {
            val song = pendingSong ?: return
            launch {
                catalogSource.hideSong(song.id)
                playbackController.removeSongsFromQueue(listOf(song.id))
                clearRemovePrompt()
                stateDelegate.reload()
            }
        }

        override fun confirmDeleteForever() {
            val song = pendingSong ?: return
            launch {
                if (catalogSource.deleteSong(song.id)) {
                    playbackController.removeSongsFromQueue(listOf(song.id))
                    stateDelegate.reload()
                }
                clearRemovePrompt()
            }
        }

        override fun dismissRemove() {
            clearRemovePrompt()
        }

        override fun onBackClicked() {
            navigator.showPreviousScreen()
        }

        private fun clearRemovePrompt() {
            pendingSong = null
            _removePrompt.value = null
        }
    }
}
