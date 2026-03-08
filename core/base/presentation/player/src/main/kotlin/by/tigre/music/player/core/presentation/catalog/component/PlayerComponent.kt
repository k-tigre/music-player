package by.tigre.music.player.core.presentation.catalog.component

import by.tigre.music.player.core.presentation.catalog.di.PlayerDependency
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext

interface PlayerComponent : BasePlayerComponent {

    fun showQueue()

    class Impl(
        context: BaseComponentContext,
        dependency: PlayerDependency,
        private val navigator: PlayerNavigator
    ) : PlayerComponent, BasePlayerComponent by BasePlayerComponentImpl(context, dependency) {

        override fun showQueue() {
            navigator.showQueue()
        }
    }
}
