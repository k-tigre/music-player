package by.tigre.music.player.core.presentation.favorites.component

import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.favorites.di.FavoritesDependency
import by.tigre.music.player.core.presentation.favorites.navigation.FavoritesNavigator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface FavoritesComponent {
    val segment: StateFlow<FavoritesSegment>
    val favoriteIds: StateFlow<Set<Song.Id>>
    val tracks: StateFlow<List<FavoritesRepository.FavoriteTrack>>
    val likedAlbums: StateFlow<List<FavoritesRepository.LikedAlbum>>
    val likedArtists: StateFlow<List<FavoritesRepository.LikedArtist>>
    val messages: SharedFlow<Message>

    fun selectSegment(segment: FavoritesSegment)
    fun onPlayAllTracks()
    fun onPlayTrack(song: Song)
    fun onAddTrackToQueue(song: Song)
    fun onToggleTrackFavorite(songId: Song.Id)
    fun onPlayAlbum(entry: FavoritesRepository.LikedAlbum)
    fun onToggleAlbumFavorite(entry: FavoritesRepository.LikedAlbum)
    fun onOpenAlbum(entry: FavoritesRepository.LikedAlbum)
    fun onPlayArtist(entry: FavoritesRepository.LikedArtist)
    fun onToggleArtistFavorite(entry: FavoritesRepository.LikedArtist)
    fun onOpenArtist(entry: FavoritesRepository.LikedArtist)

    enum class FavoritesSegment {
        Tracks, Albums, Artists,
    }

    sealed interface Message {
        data object NoPlayableTracks : Message
    }

    class Impl(
        context: BaseComponentContext,
        dependency: FavoritesDependency,
        private val navigator: FavoritesNavigator,
    ) : FavoritesComponent, BaseComponentContext by context {

        private val favoritesRepository = dependency.favoritesRepository
        private val playbackController = dependency.playbackController
        private val eventAnalytics = dependency.eventAnalytics

        private val _segment = MutableStateFlow(FavoritesSegment.Tracks)
        override val segment: StateFlow<FavoritesSegment> = _segment.asStateFlow()

        override val favoriteIds: StateFlow<Set<Song.Id>> = favoritesRepository.favoriteIds
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

        override val tracks: StateFlow<List<FavoritesRepository.FavoriteTrack>> = favoritesRepository.tracks
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        override val likedAlbums: StateFlow<List<FavoritesRepository.LikedAlbum>> = favoritesRepository.likedAlbums
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        override val likedArtists: StateFlow<List<FavoritesRepository.LikedArtist>> = favoritesRepository.likedArtists
            .stateIn(
                scope = this,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 1)
        override val messages: SharedFlow<Message> = _messages.asSharedFlow()

        override fun selectSegment(segment: FavoritesSegment) {
            _segment.value = segment
        }

        override fun onPlayAllTracks() {
            launch {
                val ids = favoritesRepository.resolvePlayableSongIds()
                if (ids.isEmpty()) {
                    _messages.emit(Message.NoPlayableTracks)
                    return@launch
                }
                playbackController.playSongs(ids)
            }
        }

        override fun onPlayTrack(song: Song) {
            playbackController.playSong(song.id)
        }

        override fun onAddTrackToQueue(song: Song) {
            playbackController.addSongToPlay(song.id)
        }

        override fun onToggleTrackFavorite(songId: Song.Id) {
            launch {
                val isFavorite = favoritesRepository.toggle(songId)
                eventAnalytics.trackEvent(MusicEvents.Action.FavoriteToggle(isFavorite))
            }
        }

        override fun onPlayAlbum(entry: FavoritesRepository.LikedAlbum) {
            playbackController.playAlbum(entry.album.id, entry.artistId)
        }

        override fun onToggleAlbumFavorite(entry: FavoritesRepository.LikedAlbum) {
            launch {
                val isFavorite = favoritesRepository.toggleAlbum(entry.artistId, entry.album.id)
                eventAnalytics.trackEvent(MusicEvents.Action.FavoriteToggle(isFavorite))
            }
        }

        override fun onOpenAlbum(entry: FavoritesRepository.LikedAlbum) {
            navigator.openAlbum(entry.artistId, entry.album.id)
        }

        override fun onPlayArtist(entry: FavoritesRepository.LikedArtist) {
            playbackController.playArtist(entry.artist.id)
        }

        override fun onToggleArtistFavorite(entry: FavoritesRepository.LikedArtist) {
            launch {
                val isFavorite = favoritesRepository.toggleArtist(entry.artist.id)
                eventAnalytics.trackEvent(MusicEvents.Action.FavoriteToggle(isFavorite))
            }
        }

        override fun onOpenArtist(entry: FavoritesRepository.LikedArtist) {
            navigator.openArtist(entry.artist.id)
        }
    }
}
