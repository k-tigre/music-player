package by.tigre.music.player.tools.coroutines.extensions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest


@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.throttleLatest(delayMillis: Long): Flow<T> = this
    .conflate()
    .transformLatest {
        delay(delayMillis)
        emit(it)
    }

fun <T> Flow<T>.throttleFirst(periodMillis: Long): Flow<T> {
    require(periodMillis > 0) { "period should be positive" }
    return flow {
        var lastTime = 0L
        collect { value ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= periodMillis) {
                lastTime = currentTime
                emit(value)
            }
        }
    }
}
