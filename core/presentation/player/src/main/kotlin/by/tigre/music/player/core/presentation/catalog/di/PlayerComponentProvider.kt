package by.tigre.music.player.core.presentation.catalog.di

import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.navigation.PlayerNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext

interface PlayerComponentProvider {
    fun createSmallPlayerComponent(context: BaseComponentContext, navigator: PlayerNavigator): SmallPlayerComponent

    class Impl(
        private val dependency: PlayerDependency
    ) : PlayerComponentProvider {
        override fun createSmallPlayerComponent(
            context: BaseComponentContext,
            navigator: PlayerNavigator
        ): SmallPlayerComponent = SmallPlayerComponent.Impl(context, dependency, navigator)
    }
}
