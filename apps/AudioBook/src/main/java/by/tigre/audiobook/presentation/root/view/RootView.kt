package by.tigre.audiobook.presentation.root.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.R
import by.tigre.audiobook.core.presentation.audiobook_catalog.di.AudiobookCatalogViewProvider
import by.tigre.audiobook.presentation.root.component.Root
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.catalog.view.EqualizerView
import by.tigre.music.player.core.presentation.catalog.view.PlayerView
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

class RootView(
    private val component: Root,
    private val playerViewProvider: PlayerViewProvider,
    private val audiobookCatalogViewProvider: AudiobookCatalogViewProvider
) : ComposableView {

    @Composable
    override fun Draw(modifier: Modifier) {
        DrawMain()
    }

    @Composable
    private fun DrawMain() {
        Children(
            stack = component.mainComponent,
            animation = stackAnimation(animator = fade())
        ) {
            when (val child = it.instance) {
                is Root.MainComponentChild.Main -> DrawPages()
                is Root.MainComponentChild.Player -> playerViewProvider.createPlayerView(
                    component = child.component,
                    config = PlayerView.Config(
                        emptyScreenAction = component::onShowCatalog,
                        emptyScreenTitle = "No book selected",
                        emptyScreenMessage = "Select a book to listen to",
                        emptyScreenActionTitle = "Select from catalog",
                        coverFallbackIcon = R.drawable.ic_launcher_foreground,
                        showOrderModeButton = false,
                        equalizerMenuLabel = stringResource(R.string.player_equalizer_menu),
                        queueMenuLabel = stringResource(R.string.player_queue_menu),
                    ),
                    topBarContent = {
                        val eqAvailable by child.component.playbackEqualizer.isAvailable.collectAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (eqAvailable) {
                                IconButton(
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    onClick = { child.component.showEqualizer() }
                                ) {
                                    Icon(
                                        contentDescription = stringResource(R.string.player_equalizer_menu),
                                        imageVector = Icons.Default.GraphicEq,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                            IconButton(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onClick = component::onShowCatalog
                            ) {
                                Icon(
                                    contentDescription = null,
                                    imageVector = Icons.Default.LocalLibrary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                ).Draw(Modifier.fillMaxSize())

                is Root.MainComponentChild.Equalizer -> playerViewProvider.createEqualizerView(
                    component = child.component,
                    config = EqualizerView.Config(
                        title = stringResource(R.string.equalizer_title),
                        factoryPresetsTableTitle = stringResource(R.string.equalizer_factory_presets_table),
                        presetPickerTitle = stringResource(R.string.equalizer_preset_picker),
                        bandsSectionTitle = stringResource(R.string.equalizer_bands),
                        customPresetLabel = stringResource(R.string.equalizer_custom),
                        unavailableMessage = stringResource(R.string.equalizer_unavailable),
                    )
                ).Draw(Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun DrawPages() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                playerViewProvider.createSmallPlayerView(
                    component = component.playerComponent,
                    showOrderModeButton = false,
                ).Draw(Modifier)
            }
        ) { paddings ->
            audiobookCatalogViewProvider.createRootView(component.audiobookCatalogComponent)
                .Draw(
                    Modifier
                        .fillMaxSize()
                        .padding(paddings)
                )
        }
    }
}
