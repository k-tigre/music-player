package by.tigre.debug_settings

import by.tigre.music.player.logger.LogsProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface DebugComponent {
    val currentPage: StateFlow<Int>
    val pages: List<DebugPageComponent>

    class Impl(
        componentContext: BaseComponentContext,
        logsProvider: LogsProvider
    ) : DebugComponent, BaseComponentContext by componentContext {

        private val logsComponent: DebugLogsComponent =
            DebugLogsComponent.Impl(appChildContext("logs"), logsProvider, null)

        private val logsAnalyticsComponent: DebugLogsComponent =
            DebugLogsComponent.Impl(appChildContext("logsAnalytics"), logsProvider, filter = "Analytics%")

        override val currentPage = MutableStateFlow(0)

        override val pages: List<DebugPageComponent> =
            listOf(logsComponent, logsAnalyticsComponent)
    }
}
