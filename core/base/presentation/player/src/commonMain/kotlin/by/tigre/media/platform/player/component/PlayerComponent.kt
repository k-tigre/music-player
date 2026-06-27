package by.tigre.media.platform.player.component

import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.media.platform.presentation.BaseComponentContext

interface PlayerComponent : BasePlayerComponent {

    fun showQueue()

    fun showEqualizer()

    fun showSettings()

    class Impl(
        context: BaseComponentContext,
        dependency: PlayerDependency,
        private val navigator: PlayerNavigator
    ) : PlayerComponent, BasePlayerComponent by BasePlayerComponentImpl(context, dependency) {

        override fun showQueue() {
            navigator.showQueue()
        }

        override fun showEqualizer() {
            navigator.showEqualizer()
        }

        override fun showSettings() {
            navigator.showSettings()
        }
    }
}
