package by.tigre.music.player.core.presentation.catalog.view

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
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
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.tools.platform.compose.AppTheme
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.player.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SmallPlayerView(
    private val component: SmallPlayerComponent,
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BoxScope.DrawProgress() {
        val position = component.fraction.collectAsState()

        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var sliderEnabled by remember { mutableStateOf(false) }
        val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
        val colors = SliderDefaults.colors(
            activeTickColor = MaterialTheme.colorScheme.secondary,
            thumbColor = MaterialTheme.colorScheme.secondary,
            activeTrackColor = MaterialTheme.colorScheme.secondary,
            inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )

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
                sliderEnabled = false
            },
            colors = colors,
            interactionSource = interactionSource,
            thumb = {
                Spacer(
                    Modifier
                        .size(24.dp)
                        .hoverable(interactionSource = interactionSource)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    colors = colors,
                    enabled = true,
                    sliderState = state,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(8.dp)
                )
            }
        )
    }

    @Composable
    private fun DrawActions() {
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isNormal = component.isNormal.collectAsState().value
            val state = component.state.collectAsState()
            val position = component.position.collectAsState().value

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "${position.current}/${position.total}",
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { component.switchMode(isNormal.not()) }
            ) {
                Icon(
                    contentDescription = null,
                    painter = painterResource(id = if (isNormal) R.drawable.ic_play_repeat_all else R.drawable.ic_play_shuffle),
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = component::prev
            ) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_skip_previous_24))
            }

            if (state.value == BasePlayerComponent.State.Playing) {
                IconButton(
                    onClick = component::pause
                ) {
                    Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_pause_24))
                }
            } else {
                IconButton(
                    onClick = component::play
                ) {
                    Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_play_arrow_24))
                }
            }

            IconButton(
                onClick = component::next
            ) {
                Icon(contentDescription = null, painter = painterResource(id = R.drawable.baseline_skip_next_24))
            }

        }
    }
}

internal object PreviewStub {
    val playerItem: PlayerItem = PlayerItem(
        title = "Song name",
        subtitle = "Test Artist/Test Album"
    )

    private fun baseComponent(item: PlayerItem?, isNormalMode: Boolean) = object : BasePlayerComponent {
        override val currentItem = MutableStateFlow(item)
        override val fraction = MutableStateFlow(0.5f)
        override val position = MutableStateFlow(BasePlayerComponent.Position("10:10", "-10:19", "10:19"))
        override val state = MutableStateFlow(BasePlayerComponent.State.Paused)
        override val isNormal: StateFlow<Boolean> = MutableStateFlow(isNormalMode)

        override fun pause() {
            state.tryEmit(BasePlayerComponent.State.Paused)
        }

        override fun play() {
            state.tryEmit(BasePlayerComponent.State.Playing)
        }

        override fun next() = Unit
        override fun prev() = Unit
        override fun switchMode(isNormal: Boolean) {
            TODO("Not yet implemented")
        }

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
            override fun navigateBack() = Unit
        }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        SmallPlayerView(
            PreviewStub.smallPlayerComponent(
                item = PreviewStub.playerItem,
                isNormalMode = false
            )
        ).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDark() {
    AppTheme {
        SmallPlayerView(
            PreviewStub.smallPlayerComponent(
                item = PreviewStub.playerItem,
                isNormalMode = true
            )
        ).Draw(modifier = Modifier.padding(top = 36.dp))
    }
}
