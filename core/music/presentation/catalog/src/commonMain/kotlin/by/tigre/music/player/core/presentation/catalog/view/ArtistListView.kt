package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.component.ArtistListScreenData
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.CardWithPopup
import by.tigre.music.player.tools.platform.compose.view.EmptyScreen
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.PopupAction
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize
import `by`.tigre.music.player.core.presentation.catalog.resources.Res
import `by`.tigre.music.player.core.presentation.catalog.resources.*
import org.jetbrains.compose.resources.stringResource

class ArtistListView(
    private val component: ArtistListComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
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
                        IconButton(onClick = component::retry) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(Res.string.cd_reload)
                            )
                        }
                    }
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()
                val searchQuery by component.searchQuery.collectAsState()
                val contentState = when (screenState) {
                    is ScreenContentState.Loading -> ArtistListUiState.Loading
                    is ScreenContentState.Error -> ArtistListUiState.Error
                    is ScreenContentState.Content -> ArtistListUiState.Content
                }

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

                    Crossfade(
                        modifier = Modifier.fillMaxSize(),
                        targetState = contentState,
                        animationSpec = tween(500),
                        label = "state"
                    ) { state ->
                        when (state) {
                            ArtistListUiState.Loading -> {
                                ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                            }

                            ArtistListUiState.Error -> {
                                ErrorScreen(retryAction = component::retry)
                            }

                            ArtistListUiState.Content -> {
                                val data = (screenState as ScreenContentState.Content).value
                                DrawContent(data)
                            }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun DrawContent(data: ArtistListScreenData) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
            } else if (data.artists.isEmpty()) {
                item {
                    EmptyScreen(
                        modifier = Modifier.padding(32.dp),
                        reloadAction = component::retry,
                        message = stringResource(Res.string.catalog_empty_artists_message)
                    )
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
        CardWithPopup(
            modifier = Modifier,
            title = artist.name,
            onCardClicked = { component.onArtistClicked(artist) },
            popupActions = listOf(
                PopupAction(stringResource(Res.string.action_play)) { component.onPlayArtistClicked(artist) },
                PopupAction(stringResource(Res.string.action_add_to_queue)) { component.onAddToPlayArtistClicked(artist) },
            ),
            descriptions = listOf(
                stringResource(Res.string.catalog_artist_albums_count, artist.albumCount),
                stringResource(Res.string.catalog_artist_songs_count, artist.songCount)
            )
        )
    }

    @Composable
    private fun SearchSongCard(song: Song) {
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
            )
        )
    }

    private enum class ArtistListUiState {
        Loading,
        Error,
        Content,
    }
}
