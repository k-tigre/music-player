package by.tigre.media.platform.player.di

import by.tigre.media.platform.player.component.EqualizerComponent
import by.tigre.media.platform.player.component.PlayerComponent
import by.tigre.media.platform.player.component.SmallPlayerComponent
import by.tigre.media.platform.player.navigation.PlayerNavigator
import by.tigre.media.platform.presentation.BaseComponentContext

interface PlayerComponentProvider {
    fun createSmallPlayerComponent(context: BaseComponentContext, navigator: PlayerNavigator): SmallPlayerComponent
    fun createPlayerComponent(context: BaseComponentContext, navigator: PlayerNavigator): PlayerComponent
    fun createEqualizerComponent(onClose: () -> Unit): EqualizerComponent

    class Impl(
        private val dependency: PlayerDependency
    ) : PlayerComponentProvider {
        override fun createSmallPlayerComponent(
            context: BaseComponentContext,
            navigator: PlayerNavigator
        ): SmallPlayerComponent = SmallPlayerComponent.Impl(context, dependency, navigator)

        override fun createPlayerComponent(
            context: BaseComponentContext,
            navigator: PlayerNavigator
        ): PlayerComponent = PlayerComponent.Impl(context, dependency, navigator)

        override fun createEqualizerComponent(onClose: () -> Unit): EqualizerComponent =
            EqualizerComponent.Impl(dependency, onClose)
    }
}
