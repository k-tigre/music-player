package by.tigre.music.player.marketing

import android.content.Context
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.player.component.BasePlayerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.PlayerItem
import by.tigre.media.platform.player.component.RepeatMode
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.entiry.playback.NowPlayingQueueEntry
import by.tigre.music.player.core.entiry.playback.NowPlayingScreenModel
import by.tigre.music.player.core.entiry.playback.QueueSession
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.entiry.playlist.PlaylistKind
import by.tigre.music.player.core.presentation.catalog.component.AlbumListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListScreenData
import by.tigre.music.player.core.presentation.catalog.component.SongsListComponent
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.library.component.PlaylistsListComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

object MarketingScreenshotFixtures {

    private val beatlesId = Artist.Id(1)
    private val floydId = Artist.Id(2)
    private val queenId = Artist.Id(3)

    private val abbeyRoadId = Album.Id(101)
    private val darkSideId = Album.Id(102)
    private val thrillerId = Album.Id(201)

    private val songComeTogetherId = Song.Id(1001)
    private val songHereComesSunId = Song.Id(1002)
    private val songMoneyId = Song.Id(2001)
    private val songTimeId = Song.Id(2002)

    private fun str(locale: MarketingScreenshotLocale, key: String): String =
        MarketingScreenshotResources.string(locale, key)

    fun albumArtProvider(context: Context, locale: MarketingScreenshotLocale): AlbumArtProvider =
        object : AlbumArtProvider {
            override fun albumArtUri(albumId: Album.Id): Any? = when (albumId) {
                abbeyRoadId -> MarketingScreenshotResources.coverUri(context, locale, "abbey_road.jpg")
                darkSideId -> MarketingScreenshotResources.coverUri(context, locale, "dark_side.jpg")
                thrillerId -> MarketingScreenshotResources.coverUri(context, locale, "thriller.jpg")
                else -> null
            }
        }

    fun artistListComponent(context: Context, locale: MarketingScreenshotLocale): ArtistListComponent =
        object : ArtistListComponent {
            override val screenState: StateFlow<ScreenContentState<ArtistListScreenData>> =
                MutableStateFlow(
                    ScreenContentState.Content(
                        ArtistListScreenData(
                            searchQuery = "",
                            artists = artists(locale),
                            searchResult = null,
                        )
                    )
                )
            override val searchQuery: StateFlow<String> = MutableStateFlow("")
            override val settingsAvailable: Boolean = true
            override val favoriteIds: StateFlow<Set<Song.Id>> = MutableStateFlow(setOf(songComeTogetherId))
            override val likedArtistIds: StateFlow<Set<Artist.Id>> = MutableStateFlow(setOf(beatlesId))
            override fun retry() = Unit
            override fun openSettings() = Unit
            override fun onSearchQueryChanged(query: String) = Unit
            override fun onArtistClicked(artist: Artist) = Unit
            override fun onAddToPlayArtistClicked(artist: Artist) = Unit
            override fun onAddArtistToPlaylistClicked(artist: Artist) = Unit
            override fun onPlayArtistClicked(artist: Artist) = Unit
            override fun onSearchSongClicked(song: Song) = Unit
            override fun onPlaySearchSongClicked(song: Song) = Unit
            override fun onAddSearchSongClicked(song: Song) = Unit
            override fun onToggleSearchSongFavorite(song: Song) = Unit
            override fun onToggleArtistFavorite(artist: Artist) = Unit
        }

    fun albumListComponent(context: Context, locale: MarketingScreenshotLocale): AlbumListComponent =
        object : AlbumListComponent {
            override val screenState: StateFlow<ScreenContentState<List<Album>>> =
                MutableStateFlow(ScreenContentState.Content(albums(locale)))
            override val artist: Artist = artists(locale).first()
            override val removePrompt = MutableStateFlow(null)
            override val favoriteIds: StateFlow<Set<Song.Id>> = MutableStateFlow(setOf(songComeTogetherId))
            override val likedAlbumIds: StateFlow<Set<Album.Id>> = MutableStateFlow(setOf(abbeyRoadId))
            override fun retry() = Unit
            override fun onAlbumClicked(album: Album) = Unit
            override fun onBackClicked() = Unit
            override fun onPlayAlbumClicked(album: Album) = Unit
            override fun onAddToPlayAlbumClicked(album: Album) = Unit
            override fun onAddAlbumToPlaylistClicked(album: Album) = Unit
            override fun onRemoveAlbumClicked(album: Album) = Unit
            override fun confirmHide() = Unit
            override fun confirmDeleteForever() = Unit
            override fun dismissRemove() = Unit
            override fun onToggleAlbumFavorite(album: Album) = Unit
        }

    fun playerComponent(context: Context, locale: MarketingScreenshotLocale): PlayerComponent =
        object : PlayerComponent {
            private val item = PlayerItem(
                title = str(locale, "screenshot_song_come_together"),
                subtitle = str(locale, "screenshot_album_abbey_road"),
                artist = str(locale, "screenshot_artist_beatles"),
                album = str(locale, "screenshot_album_abbey_road"),
                coverUri = albumArtProvider(context, locale).albumArtUri(abbeyRoadId),
            )
            override val currentItem: StateFlow<PlayerItem?> = MutableStateFlow(item)
            override val position: StateFlow<BasePlayerComponent.Position> = MutableStateFlow(
                BasePlayerComponent.Position("02:15", "-01:45", "04:00")
            )
            override val fraction: StateFlow<Float> = MutableStateFlow(0.56f)
            override val state: StateFlow<BasePlayerComponent.State> =
                MutableStateFlow(BasePlayerComponent.State.Playing)
            override val shuffleEnabled: StateFlow<Boolean> = MutableStateFlow(false)
            override val repeatMode: StateFlow<RepeatMode> = MutableStateFlow(RepeatMode.Off)
            override val playbackEqualizer: PlaybackEqualizer = StubPlaybackEqualizer
            override val appPlaybackVolume = null
            override val playbackSpeed: StateFlow<Float>? = null
            override fun pause() = Unit
            override fun play() = Unit
            override fun next() = Unit
            override fun prev() = Unit
            override fun seekBack15Seconds() = Unit
            override fun seekBack1Minute() = Unit
            override fun seekForward15Seconds() = Unit
            override fun seekForward1Minute() = Unit
            override fun toggleShuffle() = Unit
            override fun cycleRepeat() = Unit
            override fun seekTo(fraction: Float) = Unit
            override fun showQueue() = Unit
            override fun showEqualizer() = Unit
            override fun showSettings() = Unit
        }

    fun playerViewConfig(locale: MarketingScreenshotLocale): PlayerView.Config = PlayerView.Config(
        emptyScreenAction = {},
        emptyScreenTitle = str(locale, "screenshot_player_empty_title"),
        emptyScreenMessage = str(locale, "screenshot_player_empty_message"),
        emptyScreenActionTitle = str(locale, "screenshot_player_empty_action"),
        dynamicBackdropEnabled = true,
        showOrderModeButton = true,
        equalizerMenuLabel = str(locale, "screenshot_player_equalizer"),
        queueMenuLabel = str(locale, "screenshot_player_queue"),
    )

    fun currentQueueComponent(locale: MarketingScreenshotLocale): CurrentQueueComponent =
        object : CurrentQueueComponent {
            private val queueSongs = songs(locale)
            override val screenState: StateFlow<ScreenContentState<NowPlayingScreenModel>> =
                MutableStateFlow(
                    ScreenContentState.Content(
                        NowPlayingScreenModel(
                            session = QueueSession.Plain,
                            overlay = null,
                            queue = queueSongs.mapIndexed { index, song ->
                                NowPlayingQueueEntry(
                                    id = song.id.value,
                                    song = song,
                                    isPlaying = index == 0,
                                    isInterruptedActive = false,
                                )
                            },
                        )
                    )
                )
            override val scrollToPlayingTrackEvents: SharedFlow<Int> = MutableSharedFlow()
            override val saveDialogState: StateFlow<CurrentQueueComponent.SaveDialogState?> =
                MutableStateFlow(null)
            override val nameError: StateFlow<Boolean> = MutableStateFlow(false)
            override val favoriteIds: StateFlow<Set<Song.Id>> = MutableStateFlow(setOf(songComeTogetherId))
            override fun retry() = Unit
            override fun onSongClicked(entry: NowPlayingQueueEntry) = Unit
            override fun onOverlayReturnToQueueClicked() = Unit
            override fun onOverlayRowClicked() = Unit
            override fun onAddToQueueClicked() = Unit
            override fun onOpenArtistClicked(entry: NowPlayingQueueEntry) = Unit
            override fun onOpenAlbumClicked(entry: NowPlayingQueueEntry) = Unit
            override fun onAddToPlaylistClicked(entry: by.tigre.music.player.core.entiry.playback.SongInQueueItem) = Unit
            override fun onMoveTrackUp(entryId: Long) = Unit
            override fun onMoveTrackDown(entryId: Long) = Unit
            override fun onMoveTrackToTop(entryId: Long) = Unit
            override fun onMoveTrackToBottom(entryId: Long) = Unit
            override fun onTracksReordered(entryIdsInOrder: List<Long>) = Unit
            override fun onRemoveTrack(entry: NowPlayingQueueEntry) = Unit
            override fun onToggleFavorite(entry: NowPlayingQueueEntry) = Unit
            override fun onSaveClicked() = Unit
            override fun onSaveNewPlaylistConfirmed(name: String) = Unit
            override fun dismissSaveDialog() = Unit
        }

    fun playlistsListComponent(locale: MarketingScreenshotLocale): PlaylistsListComponent =
        object : PlaylistsListComponent {
            override val screenState: StateFlow<ScreenContentState<List<Playlist>>> =
                MutableStateFlow(ScreenContentState.Content(playlists(locale)))
            override val dialogState: StateFlow<PlaylistsListComponent.PlaylistsDialogState?> =
                MutableStateFlow(null)
            override val nameError: StateFlow<Boolean> = MutableStateFlow(false)
            override fun retry() = Unit
            override fun onCreateClicked() = Unit
            override fun onCreateConfirmed(name: String) = Unit
            override fun onPlaylistClicked(playlist: Playlist) = Unit
            override fun onRenameClicked(playlist: Playlist) = Unit
            override fun onRenameConfirmed(name: String) = Unit
            override fun onDeleteClicked(playlist: Playlist) = Unit
            override fun onDeleteConfirmed() = Unit
            override fun dismissDialog() = Unit
        }

    fun favoritesComponent(locale: MarketingScreenshotLocale): FavoritesComponent =
        object : FavoritesComponent {
            override val segment: StateFlow<FavoritesComponent.FavoritesSegment> =
                MutableStateFlow(FavoritesComponent.FavoritesSegment.Tracks)
            override val favoriteIds: StateFlow<Set<Song.Id>> =
                MutableStateFlow(setOf(songComeTogetherId, songHereComesSunId, songMoneyId))
            override val tracks: StateFlow<List<FavoritesRepository.FavoriteTrack>> = MutableStateFlow(
                songs(locale).take(3).mapIndexed { index, song ->
                    FavoritesRepository.FavoriteTrack(
                        songId = song.id,
                        addedAt = 1_700_000_000_000L - index * 86_400_000L,
                        song = song,
                    )
                }
            )
            override val likedAlbums: StateFlow<List<FavoritesRepository.LikedAlbum>> = MutableStateFlow(
                albums(locale).take(2).mapIndexed { index, album ->
                    FavoritesRepository.LikedAlbum(
                        artistId = beatlesId,
                        album = album,
                        addedAt = 1_700_000_000_000L - index * 172_800_000L,
                    )
                }
            )
            override val likedArtists: StateFlow<List<FavoritesRepository.LikedArtist>> = MutableStateFlow(
                artists(locale).take(2).mapIndexed { index, artist ->
                    FavoritesRepository.LikedArtist(
                        artist = artist,
                        addedAt = 1_700_000_000_000L - index * 259_200_000L,
                    )
                }
            )
            private val messagesFlow = MutableSharedFlow<FavoritesComponent.Message>()
            override val messages: SharedFlow<FavoritesComponent.Message> = messagesFlow.asSharedFlow()
            override fun selectSegment(segment: FavoritesComponent.FavoritesSegment) = Unit
            override fun onPlayAllTracks() = Unit
            override fun onPlayTrack(song: Song) = Unit
            override fun onAddTrackToQueue(song: Song) = Unit
            override fun onToggleTrackFavorite(songId: Song.Id) = Unit
            override fun onPlayAlbum(entry: FavoritesRepository.LikedAlbum) = Unit
            override fun onToggleAlbumFavorite(entry: FavoritesRepository.LikedAlbum) = Unit
            override fun onOpenAlbum(entry: FavoritesRepository.LikedAlbum) = Unit
            override fun onPlayArtist(entry: FavoritesRepository.LikedArtist) = Unit
            override fun onToggleArtistFavorite(entry: FavoritesRepository.LikedArtist) = Unit
            override fun onOpenArtist(entry: FavoritesRepository.LikedArtist) = Unit
            override fun onOpenCatalog() = Unit
        }

    private fun artists(locale: MarketingScreenshotLocale): List<Artist> = listOf(
        Artist(beatlesId, str(locale, "screenshot_artist_beatles"), 24, 3),
        Artist(floydId, str(locale, "screenshot_artist_floyd"), 18, 2),
        Artist(queenId, str(locale, "screenshot_artist_queen"), 12, 1),
    )

    private fun albums(locale: MarketingScreenshotLocale): List<Album> = listOf(
        Album(abbeyRoadId, str(locale, "screenshot_album_abbey_road"), 9, "1969"),
        Album(darkSideId, str(locale, "screenshot_album_dark_side"), 10, "1973"),
        Album(thrillerId, str(locale, "screenshot_album_thriller"), 9, "1982"),
    )

    private fun songs(locale: MarketingScreenshotLocale): List<Song> = listOf(
        Song(
            songComeTogetherId,
            str(locale, "screenshot_song_come_together"),
            "1",
            str(locale, "screenshot_artist_beatles"),
            str(locale, "screenshot_album_abbey_road"),
            beatlesId,
            abbeyRoadId,
            "/music/come_together.mp3",
        ),
        Song(
            songHereComesSunId,
            str(locale, "screenshot_song_here_comes_sun"),
            "2",
            str(locale, "screenshot_artist_beatles"),
            str(locale, "screenshot_album_abbey_road"),
            beatlesId,
            abbeyRoadId,
            "/music/here_comes_the_sun.mp3",
        ),
        Song(
            songMoneyId,
            str(locale, "screenshot_song_money"),
            "3",
            str(locale, "screenshot_artist_floyd"),
            str(locale, "screenshot_album_dark_side"),
            floydId,
            darkSideId,
            "/music/money.mp3",
        ),
        Song(
            songTimeId,
            str(locale, "screenshot_song_time"),
            "4",
            str(locale, "screenshot_artist_floyd"),
            str(locale, "screenshot_album_dark_side"),
            floydId,
            darkSideId,
            "/music/time.mp3",
        ),
    )

    private fun playlists(locale: MarketingScreenshotLocale): List<Playlist> = listOf(
        Playlist(Playlist.Id(1), str(locale, "screenshot_playlist_road_trip"), PlaylistKind.User, 42),
        Playlist(Playlist.Id(2), str(locale, "screenshot_playlist_workout"), PlaylistKind.User, 28),
        Playlist(Playlist.Id(3), str(locale, "screenshot_playlist_chill"), PlaylistKind.Smart, 15),
    )

    private object StubPlaybackEqualizer : PlaybackEqualizer {
        override val isAvailable: StateFlow<Boolean> = MutableStateFlow(false)
        override val presetNames: StateFlow<List<String>> = MutableStateFlow(emptyList())
        override val selectedPresetIndex: StateFlow<Int> = MutableStateFlow(0)
        override val bandCenterHz: StateFlow<List<Float>> = MutableStateFlow(emptyList())
        override val bandGainDb: StateFlow<List<Float>> = MutableStateFlow(emptyList())
        override val builtInPresetBandGainsDb: StateFlow<List<List<Float>>> = MutableStateFlow(emptyList())
        override val customPresetIndex: StateFlow<Int> = MutableStateFlow(-1)
        override val bandGainRangeDb: StateFlow<Pair<Float, Float>> = MutableStateFlow(-12f to 12f)
        override fun selectPreset(index: Int) = Unit
        override fun setBandGainDb(bandIndex: Int, gainDb: Float) = Unit
    }
}
