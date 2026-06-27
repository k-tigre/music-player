package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.data.catalog.AlbumArtProvider
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListScreenData
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import androidx.compose.material.icons.outlined.Person
import by.tigre.media.platform.tools.platform.compose.view.CardWithPopup
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.EmptyScreen
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.FavoriteHeartButton
import by.tigre.media.platform.tools.platform.compose.view.PopupAction
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import `by`.tigre.music.player.core.presentation.catalog.resources.Res
import `by`.tigre.music.player.core.presentation.catalog.resources.*
import org.jetbrains.compose.resources.stringResource

class ArtistListView(
    private val component: ArtistListComponent,
    private val albumArtProvider: AlbumArtProvider,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.screen_artists_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (component.settingsAvailable) {
                            IconButton(onClick = component::openSettings) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(Res.string.cd_settings)
                                )
                            }
                        }
                    }
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()
                val searchQuery by component.searchQuery.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        value = searchQuery,
                        onValueChange = component::onSearchQueryChanged,
                        singleLine = true,
                        placeholder = { Text(stringResource(Res.string.catalog_search_hint)) },
                        label = { Text(stringResource(Res.string.catalog_search_hint)) }
                    )

                    AnimatedContent(
                        modifier = Modifier.fillMaxSize(),
                        targetState = screenState,
                        contentKey = { state -> state::class },
                        label = "state",
                    ) { state ->
                        when (state) {
                            is ScreenContentState.Loading -> {
                                ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                            }

                            is ScreenContentState.Error -> {
                                ErrorScreen(retryAction = component::retry)
                            }

                            is ScreenContentState.Content -> {
                                DrawContent(state.value)
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun DrawContent(data: ArtistListScreenData) {
        if (data.searchResult == null && data.artists.isEmpty()) {
            EmptyScreen(
                modifier = Modifier.padding(32.dp),
                reloadAction = component::retry,
                message = stringResource(Res.string.catalog_empty_artists_message),
            )
            return
        }

        LazyColumn(
            contentPadding = bottomBarListContentPadding(horizontal = 0.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (data.searchResult != null) {
                val result = data.searchResult
                if (result.artists.isEmpty() && result.songs.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(vertical = 24.dp),
                            text = stringResource(Res.string.catalog_search_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    if (result.artists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.catalog_search_section_artists),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        result.artists.forEach { artist ->
                            item { ArtistCard(artist) }
                        }
                    }
                    if (result.songs.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.catalog_search_section_tracks),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        result.songs.forEach { song ->
                            item { SearchSongCard(song) }
                        }
                    }
                }
            } else {
                data.artists.forEach { artist ->
                    item { ArtistCard(artist) }
                }
            }
        }
    }

    @Composable
    private fun ArtistCard(artist: Artist) {
        val likedArtistIds by component.likedArtistIds.collectAsState()
        CardWithPopup(
            modifier = Modifier,
            title = artist.name,
            onCardClicked = { component.onArtistClicked(artist) },
            popupActions = listOf(
                PopupAction(stringResource(Res.string.action_play)) { component.onPlayArtistClicked(artist) },
                PopupAction(stringResource(Res.string.action_add_to_queue)) { component.onAddToPlayArtistClicked(artist) },
                PopupAction(stringResource(Res.string.action_add_to_playlist)) {
                    component.onAddArtistToPlaylistClicked(artist)
                },
            ),
            descriptions = listOf(
                stringResource(Res.string.catalog_artist_albums_count, artist.albumCount),
                stringResource(Res.string.catalog_artist_songs_count, artist.songCount)
            ),
            leadingContent = {
                CoverThumbnail(model = null, fallbackIcon = Icons.Outlined.Person)
            },
            trailingContent = {
                FavoriteHeartButton(
                    isFavorite = artist.id in likedArtistIds,
                    onClick = { component.onToggleArtistFavorite(artist) },
                )
            },
        )
    }

    @Composable
    private fun SearchSongCard(song: Song) {
        val favoriteIds by component.favoriteIds.collectAsState()
        CardWithPopup(
            modifier = Modifier,
            title = song.name,
            onCardClicked = { component.onSearchSongClicked(song) },
            popupActions = listOf(
                PopupAction(stringResource(Res.string.action_play)) { component.onPlaySearchSongClicked(song) },
                PopupAction(stringResource(Res.string.action_add_to_queue)) { component.onAddSearchSongClicked(song) },
            ),
            descriptions = listOf(
                stringResource(Res.string.catalog_track_meta, song.artist, song.album)
            ),
            leadingContent = {
                CoverThumbnail(model = albumArtProvider.albumArtUri(song.albumId))
            },
            trailingContent = {
                FavoriteHeartButton(
                    isFavorite = song.id in favoriteIds,
                    onClick = { component.onToggleSearchSongFavorite(song) },
                )
            },
        )
    }

}
