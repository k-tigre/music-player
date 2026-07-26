package by.tigre.music.player.core.presentation.favorites.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.media.platform.tools.platform.compose.view.FavoriteHeartButton
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.presentation.favorites.resources.Res
import by.tigre.music.player.core.presentation.favorites.resources.cd_open_equalizer
import by.tigre.music.player.core.presentation.favorites.resources.cd_open_settings
import by.tigre.music.player.core.presentation.favorites.resources.cd_player_back
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun MusicPlayerFavoriteTopBar(
    component: PlayerComponent,
    playbackController: PlaybackController,
    favoritesRepository: FavoritesRepository,
    eventAnalytics: MusicEventAnalytics,
) {
    val scope = rememberCoroutineScope()
    val eqAvailable by component.playbackEqualizer.isAvailable.collectAsState()
    val currentSong = playbackController.currentItem.collectAsState().value
    val favoriteIds by favoritesRepository.favoriteIds.collectAsState(initial = emptySet())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = component::showQueue) {
            Icon(
                contentDescription = stringResource(Res.string.cd_player_back),
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

            if (eqAvailable) {
                IconButton(onClick = component::showEqualizer) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = stringResource(Res.string.cd_open_equalizer),
                    )
                }
            }

            IconButton(onClick = component::showSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.cd_open_settings),
                )
            }
        }
    }
}
