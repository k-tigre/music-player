package by.tigre.music.player.tools.coroutines.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.scan

data class Diff<T : Any>(val prev: T?, val current: T)

fun <T : Any> Flow<T>.diff(): Flow<Diff<T>> {
    return this.scan(null as Diff<T>?) { prev, value -> Diff(prev = prev?.current, current = value) }.filterNotNull()
}
