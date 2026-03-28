package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.data.playback.PlaybackEqualizer
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.tools.platform.compose.ComposableView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmallPlayerView(
    private val component: SmallPlayerComponent,
    private val showOrderModeButton: Boolean = true,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        val current = component.currentItem.collectAsState().value
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
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.extraLarge.copy(
                                bottomStart = CornerSize(0),
                                bottomEnd = CornerSize(0)
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DrawItem(
                            modifier = Modifier
                                .animateContentSize(tween(250)),
                            item = current
                        )
                        DrawActions()
                    }
                }

                DrawProgress()
            }
        }
    }

    @Composable
    private fun DrawItem(modifier: Modifier, item: PlayerItem) {
        Column(
            modifier = modifier
                .systemBarsPadding()
                .fillMaxWidth()
                .clickable { component.showPlayerView() },
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = item.title,
            )

            Text(
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    @Composable
    private fun BoxScope.DrawProgress() {
        val position = component.fraction.collectAsState()

        PlayerProgressSlider(
            fraction = position.value,
            onSeekTo = component::seekTo,
            modifier = Modifier
                .offset(y = (-22).dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
        )
    }

    @Composable
    private fun DrawActions() {
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val state = component.state.collectAsState()
            val position = component.position.collectAsState().value

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "${position.current}/${position.total}",
            )

            Spacer(Modifier.weight(1f))

            if (showOrderModeButton) {
                val isNormal = component.isNormal.collectAsState().value
                IconButton(
                    onClick = { component.switchMode(isNormal.not()) }
                ) {
                    Icon(
                        contentDescription = null,
                        imageVector = if (isNormal) Icons.Default.Repeat else Icons.Default.Shuffle,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
            }

            IconButton(
                onClick = component::prev
            ) {
                Icon(contentDescription = null, imageVector = Icons.Default.SkipPrevious)
            }

            if (state.value == BasePlayerComponent.State.Playing) {
                IconButton(
                    onClick = component::pause
                ) {
                    Icon(contentDescription = null, imageVector = Icons.Default.Pause)
                }
            } else {
                IconButton(
                    onClick = component::play
                ) {
                    Icon(contentDescription = null, imageVector = Icons.Default.PlayArrow)
                }
            }

            IconButton(
                onClick = component::next
            ) {
                Icon(contentDescription = null, imageVector = Icons.Default.SkipNext)
            }

        }
    }
}

internal object PreviewStub {
    val playerItem: PlayerItem = PlayerItem(
        title = "Song name",
        subtitle = "Test Artist/Test Album"
    )

    private val previewEqualizer: PlaybackEqualizer = object : PlaybackEqualizer {
        override val isAvailable = MutableStateFlow(false).asStateFlow()
        override val presetNames = MutableStateFlow<List<String>>(emptyList()).asStateFlow()
        override val selectedPresetIndex = MutableStateFlow(0).asStateFlow()
        override fun selectPreset(index: Int) = Unit
    }

    private fun baseComponent(item: PlayerItem?, isNormalMode: Boolean) = object : BasePlayerComponent {
        override val currentItem = MutableStateFlow(item)
        override val fraction = MutableStateFlow(0.5f)
        override val position = MutableStateFlow(BasePlayerComponent.Position("10:10", "-10:19", "10:19"))
        override val state = MutableStateFlow(BasePlayerComponent.State.Paused)
        override val isNormal: StateFlow<Boolean> = MutableStateFlow(isNormalMode)

        override val playbackEqualizer: PlaybackEqualizer = previewEqualizer

        override fun pause() {
            state.tryEmit(BasePlayerComponent.State.Paused)
        }

        override fun play() {
            state.tryEmit(BasePlayerComponent.State.Playing)
        }

        override fun next() = Unit
        override fun prev() = Unit
        override fun switchMode(isNormal: Boolean) = Unit

        override fun seekTo(fraction: Float) {
            this.fraction.tryEmit(fraction)
        }
    }

    fun smallPlayerComponent(item: PlayerItem? = null, isNormalMode: Boolean = false): SmallPlayerComponent =
        object : SmallPlayerComponent, BasePlayerComponent by baseComponent(item, isNormalMode) {
            override fun showPlayerView() = Unit
        }

    fun playerComponent(item: PlayerItem? = null, isNormalMode: Boolean = false): PlayerComponent =
        object : PlayerComponent, BasePlayerComponent by baseComponent(item, isNormalMode) {
            override fun showQueue() = Unit
        }
}
