package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext

interface SmallPlayerComponent : BasePlayerComponent {

    fun showPlayerView()

    class Impl(
        context: BaseComponentContext,
        dependency: PlayerDependency,
        private val navigator: PlayerNavigator
    ) : SmallPlayerComponent, BasePlayerComponent by BasePlayerComponentImpl(context, dependency) {

        override fun showPlayerView() {
            navigator.playerView()
        }

    }
}
