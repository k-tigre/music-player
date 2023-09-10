package by.tigre.music.player.logger.extensions

import by.tigre.music.player.logger.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart

fun <T : Any?> Flow<T>.debugLog(name: String): Flow<T> = this
    .onStart { Log.d { "$name: doOnStart" } }
    .onEach { Log.v { "$name: onEach: $it" } }
    .catch { Log.w(it) { "$name: catch: $it" } }
    .onCompletion { Log.d { "$name: onCompletion - $it" } }
    .onEmpty { Log.d { "$name: onEmpty" } }

fun <T : Any?> Flow<T>.debugLog(tag: String, name: String): Flow<T> = this
    .onStart { Log.d(tag) { "$name: doOnStart" } }
    .onEach { Log.v(tag) { "$name: onEach: $it" } }
    .catch { Log.w(it, tag) { "$name: catch: $it" } }
    .onCompletion { Log.d(tag) { "$name: onCompletion - $it" } }
    .onEmpty { Log.d(tag) { "$name: onEmpty" } }
