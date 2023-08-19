package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.SongsListComponent
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize

class SongsListView(
    private val component: SongsListComponent,
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
                                text = "Songs of",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = component.album.name,
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
                    actions = {
                        IconButton(onClick = component::retry) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reload"
                            )
                        }
                    }
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
                    label = ""
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
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DrawContent(songs: List<Song>) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            songs.forEach { song ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { component.onSongClicked(song) },
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = song.name,
                        )

                        Text(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp),
                            text = "${song.artist}/${song.album}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
