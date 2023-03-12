package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.presentation.catalog.component.AlbumListComponent
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize

class AlbumListView(
    private val component: AlbumListComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(Modifier.padding(horizontal = 48.dp)) {
                            Text(
                                text = "Albums of",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = component.artist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = component::onBackClicked) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()

                Crossfade(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    targetState = screenState,
                    animationSpec = tween(500),
                    label = "state"
                ) { state ->

                    when (state) {
                        is ScreenContentState.Loading -> {
                            ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                        }

                        is ScreenContentState.Error -> {
                            ErrorScreen(retryAction = component::retry)
                        }

                        is ScreenContentState.Content -> {
                            DrawContent(albums = state.value)
                        }
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DrawContent(albums: List<Album>) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            albums.forEach { album ->
                item {
                    DrawItem(album = album)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun DrawItem(album: Album) {
        var popupControl by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = {
                        popupControl = true
                    },
                    onClick = { component.onAlbumClicked(album) }
                ),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = album.name,
            )

            album.years?.let { years ->
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "Years: $years"
                )
            }

            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "Songs: ${album.songCount}"
            )

            Spacer(modifier = Modifier.size(8.dp))

            if (popupControl) {
                Popup(
                    alignment = Alignment.CenterEnd,
                    onDismissRequest = { popupControl = false },
                    offset = IntOffset(-20, 0),
                ) {
                    TextButton(
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                        onClick = {
                            popupControl = false
                            component.onPlayAlbumClicked(album)
                        },
                    ) {
                        Text(
                            text = "Play",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
