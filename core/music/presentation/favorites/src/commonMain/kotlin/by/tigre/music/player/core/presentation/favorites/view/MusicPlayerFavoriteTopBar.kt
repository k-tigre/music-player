package by.tigre.music.player.core.presentation.favorites.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.playback.PlaybackController
import androidx.compose.runtime.rememberCoroutineScope
import by.tigre.media.platform.tools.platform.compose.view.FavoriteHeartButton
import kotlinx.coroutines.launch

@Composable
fun MusicPlayerFavoriteTopBar(
    component: PlayerComponent,
    config: PlayerView.Config,
    playbackController: PlaybackController,
    favoritesRepository: FavoritesRepository,
    eventAnalytics: MusicEventAnalytics,
) {
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val eqAvailable = component.playbackEqualizer.isAvailable.collectAsState()
    val presetNames = component.playbackEqualizer.presetNames.collectAsState()
    val currentSong = playbackController.currentItem.collectAsState().value
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = component::showQueue) {
            Icon(
                contentDescription = null,
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (currentSong != null) {
            FavoriteHeartButton(
                isFavorite = currentSong.id in favoriteIds,
                onClick = {
                    scope.launch {
                        val isFavorite = favoritesRepository.toggle(currentSong.id)
                        eventAnalytics.trackEvent(MusicEvents.Action.FavoriteToggle(isFavorite))
                    }
                },
            )
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    contentDescription = null,
                    imageVector = Icons.Default.MoreVert,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (eqAvailable.value && presetNames.value.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = config.equalizerMenuLabel,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            component.showEqualizer()
                        },
                    )
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text(config.queueMenuLabel) },
                    onClick = {
                        menuExpanded = false
                        component.showQueue()
                    },
                )
                config.settingsMenuLabel?.let { settingsMenuLabel ->
                    DropdownMenuItem(
                        text = { Text(settingsMenuLabel) },
                        onClick = {
                            menuExpanded = false
                            component.showSettings()
                        },
                    )
                }
            }
        }
    }
}
