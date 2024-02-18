package by.tigre.music.player.core.presentation.catalog.view

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CornerSize
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
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.tools.platform.compose.AppTheme
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.playerplayer.R
import kotlinx.coroutines.flow.MutableStateFlow

class SmallPlayerView(
    private val component: SmallPlayerComponent,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val current = component.currentSong.collectAsState().value
        if (current != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .offset(y = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.extraLarge.copy(
                                bottomStart = CornerSize(0),
                                bottomEnd = CornerSize(0)
                            )
                        )
                ) {
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        DrawItem(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(tween(250)),
                            song = current
                        )
                        DrawActions()
                    }
                }

                DrawProgress()
            }
        }
    }

    @Composable
    private fun DrawItem(modifier: Modifier, song: Song) {
        Row(
            modifier = modifier
                .systemBarsPadding()
                .clickable { component.showQueue() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                Text(
                    modifier = Modifier,
                    text = song.name,
                )

                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "${song.artist}/${song.album}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    @Composable
    private fun BoxScope.DrawProgress() {
        val position = component.position.collectAsState()

        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var sliderEnabled by remember { mutableStateOf(false) }

        Slider(
            modifier = Modifier
                .offset(y = (-22).dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            value = if (sliderEnabled) sliderPosition else position.value,
            onValueChange = {
                sliderPosition = it
                sliderEnabled = true
                component.seekTo(it)
            },
            onValueChangeFinished = {
                component.seekTo(fraction = sliderPosition)
                sliderEnabled = false
            },
            colors = SliderDefaults.colors(
                activeTickColor = MaterialTheme.colorScheme.secondary,
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        )
    }

    @Composable
    private fun RowScope.DrawActions() {
        val state = component.state.collectAsState()

        IconButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = component::prev
        ) {
            Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_skip_previous_24))
        }

        if (state.value == SmallPlayerComponent.State.Playing) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = component::pause
            ) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_pause_24))
            }
        } else {
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = component::play
            ) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_play_arrow_24))
            }
        }

        IconButton(
            modifier = Modifier.align(Alignment.CenterVertically),
            onClick = component::next
        ) {
            Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_skip_next_24))
        }
    }
}

private object PreviewStub {
    val component = object : SmallPlayerComponent {
        override val currentSong = MutableStateFlow(
            Song(
                id = Song.Id(1),
                album = "Test Album",
                artist = "Test Artist",
                index = "2/10",
                name = "Song name",
                path = ""
            )
        )
        override val position = MutableStateFlow(0.5f)
        override val state = MutableStateFlow(SmallPlayerComponent.State.Paused)

        override fun pause() {
            state.tryEmit(SmallPlayerComponent.State.Paused)
        }

        override fun play() {
            state.tryEmit(SmallPlayerComponent.State.Playing)
        }

        override fun next() = Unit
        override fun prev() = Unit

        override fun seekTo(fraction: Float) {
            position.tryEmit(fraction)
        }

        override fun showQueue() = Unit
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SmallPlayerView(PreviewStub.component).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDark() {
    AppTheme {
        SmallPlayerView(PreviewStub.component).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}
