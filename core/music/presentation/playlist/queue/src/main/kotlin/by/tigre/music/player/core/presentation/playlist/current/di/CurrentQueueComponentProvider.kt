package by.tigre.music.player.core.presentation.playlist.current.di

import by.tigre.music.player.core.presentation.playlist.current.component.CurrentQueueComponent
import by.tigre.music.player.core.presentation.playlist.current.navigation.QueueNavigator
import by.tigre.music.player.presentation.base.BaseComponentContext

interface CurrentQueueComponentProvider {
    fun createCurrentQueueComponent(context: BaseComponentContext, navigator: QueueNavigator): CurrentQueueComponent

    class Impl(
        private val dependency: CurrentQueueDependency
    ) : CurrentQueueComponentProvider {

        override fun createCurrentQueueComponent(
            context: BaseComponentContext,
            navigator: QueueNavigator
        ): CurrentQueueComponent = CurrentQueueComponent.Impl(context, dependency, navigator)
    }

}
