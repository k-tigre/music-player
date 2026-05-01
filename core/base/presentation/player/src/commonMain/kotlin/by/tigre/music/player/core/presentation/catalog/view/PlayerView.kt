package by.tigre.music.player.core.presentation.catalog.view

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.component.BasePlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerComponent
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.EmptyScreen
import coil3.compose.AsyncImage


class PlayerView(
    private val component: PlayerComponent,
    private val config: Config,
    private val topBarContent: (@Composable () -> Unit)? = null,
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        Column(modifier = modifier.fillMaxSize()) {
            topBarContent?.invoke()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (topBarContent == null) Modifier.systemBarsPadding()
                        else Modifier.navigationBarsPadding()
                    ),
                contentAlignment = Alignment.Center
            ) {
                val current = component.currentItem.collectAsState().value
                if (current != null) {
                    DrawItem(
                        modifier = Modifier.fillMaxSize(),
                        item = current
                    )
                } else {
                    EmptyScreen(
                        reloadAction = config.emptyScreenAction,
                        title = config.emptyScreenTitle,
                        message = config.emptyScreenMessage,
                        actionTitle = config.emptyScreenActionTitle
                    )
                }
                if (topBarContent == null) {
                    IconButton(
                        modifier = Modifier.align(Alignment.TopStart),
                        onClick = component::showQueue
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack
                        )
                    }

                    var menuExpanded by remember { mutableStateOf(false) }
                    val eqAvailable = component.playbackEqualizer.isAvailable.collectAsState()
                    val presetNames = component.playbackEqualizer.presetNames.collectAsState()

                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                contentDescription = null,
                                imageVector = Icons.Default.MoreVert
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (eqAvailable.value && presetNames.value.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = config.equalizerMenuLabel,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        component.showEqualizer()
                                    }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(config.queueMenuLabel) },
                                onClick = {
                                    menuExpanded = false
                                    component.showQueue()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BoxScope.DrawItem(modifier: Modifier, item: PlayerItem) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            AsyncImage(
                model = item.coverUri, contentDescription = "",
                modifier = Modifier
                    .padding(top = 48.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            DrawProgress(Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.weight(0.5f))
            DrawActions(Modifier)
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun DrawProgress(modifier: Modifier) {
        val fraction = component.fraction.collectAsState()

        PlayerProgressSlider(
            fraction = fraction.value,
            onSeekTo = component::seekTo,
            onSeekCommitted = component::onSeekCommitted,
            modifier = modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = component::prev
            ) {
                Icon(
                    contentDescription = null,
                    imageVector = Icons.Default.SkipPrevious,
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
                        imageVector = Icons.Default.Pause,
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
                        imageVector = Icons.Default.PlayArrow,
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
                    imageVector = Icons.Default.SkipNext,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (config.showOrderModeButton) {
                val isNormal = component.isNormal.collectAsState().value

                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = { component.switchMode(isNormal.not()) }
                ) {
                    Icon(
                        contentDescription = null,
                        imageVector = if (isNormal) Icons.Default.Repeat else Icons.Default.Shuffle,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    data class Config(
        val emptyScreenAction: () -> Unit,
        val emptyScreenTitle: String,
        val emptyScreenMessage: String,
        val emptyScreenActionTitle: String,
        val coverFallbackIcon: Int,
        val showOrderModeButton: Boolean = true,
        val equalizerMenuLabel: String = "Equalizer",
        val queueMenuLabel: String = "Queue",
    )
}
