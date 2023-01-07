package by.tigre.music.player.android.extension

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun awaitClose(action: () -> Unit): Unit = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { action() }
}

fun <T : Any?> Flow<T>.log(name: String): Flow<T> = this
    .onStart { Log.d(name, "$name: doOnStart") }
    .onEach { Log.v(name, "$name: onEach: $it") }
    .catch { Log.w(name, "$name: catch: $it", it) }
    .onCompletion { Log.d(name, "$name: onCompletion - $it") }
    .onEmpty { Log.d(name, "$name: onEmpty") }

fun tickerFlow(period: Long, initialDelay: Long = period) = flow {
    var value = 0
    delay(initialDelay)
    while (currentCoroutineContext().isActive) {
        emit(value)
        value += 1
        delay(period)
    }
}
