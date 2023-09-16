package by.tigre.debug_settings

import by.tigre.music.player.logger.LogsProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import bytigremusicplayerloggerdb.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal interface DebugLogsComponent : DebugPageComponent {

    val logs: Flow<List<Logs>>
    val loading: StateFlow<Boolean>

    fun onRefresh()

    @OptIn(ExperimentalCoroutinesApi::class)
    class Impl(
        componentContext: BaseComponentContext,
        private val logsProvider: LogsProvider,
        filter: String?
    ) : DebugLogsComponent, BaseComponentContext by componentContext {
        private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        override val title: String = filter ?: "Logs"

        override val logs = MutableSharedFlow<List<Logs>>()

        override val loading = MutableStateFlow(true)

        init {
            launch(Dispatchers.IO) {
                refreshSignal
                    .onStart {
                        delay(1000)
                        emit(Unit)
                    }
                    .onEach { loading.emit(true) }
                    .flatMapLatest {
                        if (filter != null) logsProvider.getLogsFlow(0, filter) else logsProvider.getLogsFlow(0)
                    }
                    .onEach { loading.emit(false) }
                    .collect(logs)
            }
        }

        override fun onRefresh() {
            refreshSignal.tryEmit(Unit)
        }
    }
}
