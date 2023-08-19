package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueComponentProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface Root {

    val playerComponent: SmallPlayerComponent
    val catalogComponent: RootCatalogComponent
    val currentQueueComponent: CurrentQueueComponent

    val onStartServiceEvent: Flow<Unit>

    val isCurrentQueueVisible: StateFlow<Boolean>

    @OptIn(FlowPreview::class)
    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider,
        currentQueueComponent: CurrentQueueComponentProvider,
        dependency: PlayerBackgroundDependency
    ) : Root, BaseComponentContext by context {

        private val playbackController = dependency.playbackController

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(
                appChildContext("player"),
                navigator = { isCurrentQueueVisible.tryEmit(true) })
        }

        override val catalogComponent: RootCatalogComponent by lazy {
            catalogComponentProvider.createRootCatalogComponent(appChildContext("catalog"))
        }

        override val currentQueueComponent: CurrentQueueComponent by lazy {
            currentQueueComponent.createCurrentQueueComponent(appChildContext("queue"),
                navigator = { isCurrentQueueVisible.tryEmit(false) })
        }

        override val isCurrentQueueVisible = MutableStateFlow(true)
        override val onStartServiceEvent = MutableSharedFlow<Unit>()

        init {
            launch {
                playbackController.player.state
                    .map { it == PlaybackPlayer.State.Playing }
                    .debounce(50)
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { onStartServiceEvent.emit(Unit) }
            }
        }

    }
}
