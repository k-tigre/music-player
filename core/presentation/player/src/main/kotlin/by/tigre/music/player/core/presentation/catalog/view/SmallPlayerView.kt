package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.entiry.catalog.Song
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.playerplayer.R

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
                    .offset(y = -0.dp)
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
//                        .padding(bottom = 24.dp)
                ) {

                    Row {
                        DrawItem(song = current, modifier = Modifier.weight(1f))
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
            modifier = modifier.systemBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // image

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

        var sliderPosition by remember { mutableStateOf(0f) }
        var sliderEnabled by remember { mutableStateOf(false) }

        Slider(
            modifier = Modifier
                .offset(y = -24.dp)
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
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
        )
    }

    @Composable
    private fun DrawActions() {
        val state = component.state.collectAsState()

        if (state.value == SmallPlayerComponent.State.Playing) {
            IconButton(onClick = component::pause) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_pause_24))
            }
        } else {
            IconButton(onClick = component::play) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_play_arrow_24))
            }
        }

        IconButton(onClick = component::next) {
            Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_skip_next_24))
        }
    }
}
