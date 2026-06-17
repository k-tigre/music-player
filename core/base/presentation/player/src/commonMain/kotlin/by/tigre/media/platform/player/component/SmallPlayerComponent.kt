package by.tigre.media.platform.player.component

import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.media.platform.presentation.BaseComponentContext

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
