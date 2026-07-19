package by.tigre.music.player.core.presentation.favorites.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.FavoriteHeartButton
import by.tigre.media.platform.tools.platform.compose.view.LocalBottomBarHeight
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.data.catalog.ArtistArtProvider
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.view.ArtistArtThumbnail
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent.FavoritesSegment
import by.tigre.music.player.core.presentation.favorites.component.FavoritesComponent.Message
import `by`.tigre.music.player.core.presentation.catalog.resources.Res as CatalogRes
import `by`.tigre.music.player.core.presentation.catalog.resources.action_add_to_queue
import `by`.tigre.music.player.core.presentation.catalog.resources.action_play
import `by`.tigre.music.player.core.presentation.catalog.resources.catalog_track_meta
import `by`.tigre.music.player.core.presentation.favorites.resources.Res
import `by`.tigre.music.player.core.presentation.favorites.resources.*
import org.jetbrains.compose.resources.stringResource

private const val FavoritesItemAnimationMs = 280

class FavoritesView(
    private val component: FavoritesComponent,
    private val albumArtProvider: AlbumArtProvider,
    private val artistArtProvider: ArtistArtProvider,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val segment by component.segment.collectAsState()
        val favoriteIds by component.favoriteIds.collectAsState()
        val tracks by component.tracks.collectAsState()
        val likedAlbums by component.likedAlbums.collectAsState()
        val likedArtists by component.likedArtists.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val noPlayableTracksText = stringResource(Res.string.favorites_no_playable_tracks)

        LaunchedEffect(Unit) {
            component.messages.collect { message ->
                when (message) {
                    Message.NoPlayableTracks -> snackbarHostState.showSnackbar(noPlayableTracksText)
                }
            }
        }

        Box(modifier = modifier) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(Res.string.nav_favorites),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            if (segment == FavoritesSegment.Tracks && tracks.any { it.song != null }) {
                                TextButton(onClick = component::onPlayAllTracks) {
                                    Text(stringResource(Res.string.favorites_play_all))
                                }
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = segment == FavoritesSegment.Tracks,
                            onClick = { component.selectSegment(FavoritesSegment.Tracks) },
                            label = { Text(stringResource(Res.string.favorites_segment_tracks)) },
                        )
                        FilterChip(
                            selected = segment == FavoritesSegment.Albums,
                            onClick = { component.selectSegment(FavoritesSegment.Albums) },
                            label = { Text(stringResource(Res.string.favorites_segment_albums)) },
                        )
                        FilterChip(
                            selected = segment == FavoritesSegment.Artists,
                            onClick = { component.selectSegment(FavoritesSegment.Artists) },
                            label = { Text(stringResource(Res.string.favorites_segment_artists)) },
                        )
                    }

                    when (segment) {
                        FavoritesSegment.Tracks -> DrawTracks(tracks, favoriteIds)
                        FavoritesSegment.Albums -> DrawAlbums(likedAlbums)
                        FavoritesSegment.Artists -> DrawArtists(likedArtists)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DrawTracks(
        tracks: List<FavoritesRepository.FavoriteTrack>,
        favoriteIds: Set<Song.Id>,
    ) {
        val playableTracks = tracks.filter { it.song != null }
        FavoritesListAnimatedContent(isEmpty = playableTracks.isEmpty()) {
            LazyColumn(
                contentPadding = bottomBarListContentPadding(horizontal = 0.dp, top = 8.dp, extraBottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(playableTracks, key = { it.songId.value }) { entry ->
                    val song = entry.song ?: return@items
                    val playAction = PopupAction(stringResource(CatalogRes.string.action_play)) {
                        component.onPlayTrack(song)
                    }
                    val addToQueueAction = PopupAction(stringResource(CatalogRes.string.action_add_to_queue)) {
                        component.onAddTrackToQueue(song)
                    }
                    CardWithPopup(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(FavoritesItemAnimationMs),
                            fadeOutSpec = tween(FavoritesItemAnimationMs),
                            placementSpec = tween(FavoritesItemAnimationMs),
                        ),
                        title = "${song.index} - ${song.name}",
                        onCardClicked = { component.onPlayTrack(song) },
                        popupActions = listOf(playAction, addToQueueAction),
                        descriptions = listOf(
                            stringResource(CatalogRes.string.catalog_track_meta, song.artist, song.album)
                        ),
                        leadingContent = {
                            CoverThumbnail(model = albumArtProvider.albumArtUri(song.albumId))
                        },
                        trailingContent = {
                            FavoriteHeartButton(
                                isFavorite = song.id in favoriteIds,
                                onClick = { component.onToggleTrackFavorite(song.id) },
                            )
                        },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DrawAlbums(albums: List<FavoritesRepository.LikedAlbum>) {
        FavoritesListAnimatedContent(isEmpty = albums.isEmpty()) {
            LazyColumn(
                contentPadding = bottomBarListContentPadding(horizontal = 0.dp, top = 8.dp, extraBottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(albums, key = { "${it.artistId.value}_${it.album.id.value}" }) { entry ->
                    CardWithPopup(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(FavoritesItemAnimationMs),
                            fadeOutSpec = tween(FavoritesItemAnimationMs),
                            placementSpec = tween(FavoritesItemAnimationMs),
                        ),
                        title = entry.album.name,
                        onCardClicked = { component.onOpenAlbum(entry) },
                        popupActions = listOf(
                            PopupAction(stringResource(CatalogRes.string.action_play)) {
                                component.onPlayAlbum(entry)
                            },
                        ),
                        descriptions = buildList {
                            add(stringResource(Res.string.favorites_album_songs_count, entry.album.songCount))
                            if (entry.album.years.isNotBlank()) {
                                add(entry.album.years)
                            }
                        },
                        leadingContent = {
                            CoverThumbnail(model = albumArtProvider.albumArtUri(entry.album.id))
                        },
                        trailingContent = {
                            FavoriteHeartButton(
                                isFavorite = true,
                                onClick = { component.onToggleAlbumFavorite(entry) },
                            )
                        },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DrawArtists(artists: List<FavoritesRepository.LikedArtist>) {
        FavoritesListAnimatedContent(isEmpty = artists.isEmpty()) {
            LazyColumn(
                contentPadding = bottomBarListContentPadding(horizontal = 0.dp, top = 8.dp, extraBottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(artists, key = { it.artist.id.value }) { entry ->
                    CardWithPopup(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(FavoritesItemAnimationMs),
                            fadeOutSpec = tween(FavoritesItemAnimationMs),
                            placementSpec = tween(FavoritesItemAnimationMs),
                        ),
                        title = entry.artist.name,
                        onCardClicked = { component.onOpenArtist(entry) },
                        popupActions = listOf(
                            PopupAction(stringResource(CatalogRes.string.action_play)) {
                                component.onPlayArtist(entry)
                            },
                        ),
                        descriptions = listOf(
                            stringResource(Res.string.favorites_artist_albums_count, entry.artist.albumCount),
                            stringResource(Res.string.favorites_artist_songs_count, entry.artist.songCount),
                        ),
                        leadingContent = {
                            ArtistArtThumbnail(
                                artistId = entry.artist.id,
                                name = entry.artist.name,
                                artistArtProvider = artistArtProvider,
                            )
                        },
                        trailingContent = {
                            FavoriteHeartButton(
                                isFavorite = true,
                                onClick = { component.onToggleArtistFavorite(entry) },
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun FavoritesListAnimatedContent(
        isEmpty: Boolean,
        content: @Composable () -> Unit,
    ) {
        AnimatedContent(
            targetState = isEmpty,
            transitionSpec = {
                fadeIn(tween(FavoritesItemAnimationMs)) togetherWith fadeOut(tween(FavoritesItemAnimationMs))
            },
            label = "favorites-list",
        ) { empty ->
            if (empty) {
                EmptyScreen(
                    title = stringResource(Res.string.favorites_empty_title),
                    message = stringResource(Res.string.favorites_empty_message),
                    actionTitle = stringResource(Res.string.favorites_empty_action),
                    icon = Icons.Outlined.FavoriteBorder,
                    reloadAction = component::onOpenCatalog,
                )
            } else {
                content()
            }
        }
    }
}
