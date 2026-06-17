package by.tigre.media.platform.tools.coroutines.extensions

import by.tigre.logger.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart

fun <T : Any?> Flow<T>.log(name: String): Flow<T> = this
    .onStart { Log.d(name, "$name: doOnStart") }
    .onEach { Log.v(name, "$name: onEach: $it") }
    .catch { Log.w(it, name, "$name: catch: $it") }
    .onCompletion { Log.d(name, "$name: onCompletion - $it") }
    .onEmpty { Log.d(name, "$name: onEmpty") }
