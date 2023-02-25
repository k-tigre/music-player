package by.tigre.music.player.tools.coroutines.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart

fun <T : Any?> Flow<T>.log(name: String): Flow<T> = this
    .onStart { println("$name: doOnStart") }
    .onEach { println("$name: onEach: $it") }
    .catch { println("$name: catch: $it") }
    .onCompletion { println("$name: onCompletion - $it") }
    .onEmpty { println("$name: onEmpty") }
