package by.tigre.music.player.presentation.base

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenContentStateDelegate<T, Data>(
    scope: CoroutineScope,
    loadData: () -> Flow<Data>,
    mapDataToState: (Data) -> ScreenContentState<T>
) {
    private val reloadSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val dataFlow: Flow<Data> = reloadSignal
        .onStart { emit(Unit) }
        .flatMapLatest { loadData() }
        .shareIn(scope, SharingStarted.WhileSubscribed())
        .onEach { Log.w("TIGRE", " - dataFlow - $it") }

    val screenState: StateFlow<ScreenContentState<T>> = merge(
        reloadSignal.map { ScreenContentState.Loading },
        dataFlow.map(mapDataToState)
    ).onEach { Log.w("TIGRE", " - screenState - $it") }
        .stateIn(scope, SharingStarted.WhileSubscribed(), ScreenContentState.Loading)


    fun reload() {
        reloadSignal.tryEmit(Unit)
    }
}
