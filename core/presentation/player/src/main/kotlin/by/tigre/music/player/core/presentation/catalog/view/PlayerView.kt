package by.tigre.music.player.core.presentation.catalog.view

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.tools.platform.compose.AppTheme
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.EmptyScreen
import by.tigre.music.playerplayer.R
import coil.compose.AsyncImage
import by.tigre.music.playercompose.R as CoreR


class PlayerView(
    private val component: PlayerComponent,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            val current = component.currentSong.collectAsState().value
            if (current != null) {
                DrawItem(
                    modifier = modifier.fillMaxSize(),
                    song = current
                )
            } else {
                EmptyScreen(
                    reloadAction = component::showQueue,
                    title = "No songs in current playlist",
                    message = "Select some track for playing",
                    actionTitle = "Select from catalog"
                )
            }
        }
    }

    @Composable
    private fun BoxScope.DrawItem(modifier: Modifier, song: Song) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                song.albumId.value
            )
            AsyncImage(
                model = contentUri, contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                placeholder = painterResource(id = R.drawable.ic_player_no_cover),
                fallback = painterResource(id = R.drawable.ic_player_no_cover)
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "${song.artist}/${song.album}",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            DrawProgress(Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.weight(0.5f))
            DrawActions(Modifier)
            Spacer(modifier = Modifier.weight(1f))
        }

        IconButton(
            modifier = Modifier.align(Alignment.TopStart),
            onClick = component::showQueue
        ) {
            Icon(
                contentDescription = null,
                painter = painterResource(id = CoreR.drawable.baseline_arrow_back_24)
            )
        }

        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = component::showQueue
        ) {
            Icon(
                contentDescription = null,
                painter = painterResource(id = CoreR.drawable.baseline_more_vert_24)
            )
        }
    }

    @Composable
    private fun DrawProgress(modifier: Modifier) {
        val fraction = component.fraction.collectAsState()

        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var sliderEnabled by remember { mutableStateOf(false) }

        Slider(
            modifier = modifier
                .fillMaxWidth(),
            value = if (sliderEnabled) sliderPosition else fraction.value,
            onValueChange = {
                sliderPosition = it
                sliderEnabled = true
                component.seekTo(it)
            },
            onValueChangeFinished = {
                sliderEnabled = false
            },
            colors = SliderDefaults.colors(
                activeTickColor = MaterialTheme.colorScheme.secondary,
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        )

        Row(
            modifier = Modifier.offset(y = (-12).dp)
        ) {
            val position = component.position.collectAsState()
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = position.value.current,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = position.value.left,
            )
        }
    }

    @Composable
    private fun DrawActions(modifier: Modifier) {
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val state = component.state.collectAsState()

            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = component::prev
            ) {
                Icon(
                    contentDescription = null,
                    painter = painterResource(id = R.drawable.baseline_skip_previous_24),
                    modifier = Modifier.size(56.dp)
                )
            }

            if (state.value == BasePlayerComponent.State.Playing) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = component::pause
                ) {
                    Icon(
                        contentDescription = null,
                        painter = painterResource(id = R.drawable.baseline_pause_24),
                        modifier = Modifier.size(56.dp)
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = component::play
                ) {
                    Icon(
                        contentDescription = null,
                        painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = component::next
            ) {
                Icon(
                    contentDescription = null,
                    painter = painterResource(id = R.drawable.baseline_skip_next_24),
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}


@Preview(showSystemUi = true)
@Composable
private fun Preview() {
    AppTheme {
        PlayerView(PreviewStub.playerComponent(PreviewStub.song)).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewEmptyDark() {
    AppTheme {
        PlayerView(PreviewStub.playerComponent(song = null)).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}