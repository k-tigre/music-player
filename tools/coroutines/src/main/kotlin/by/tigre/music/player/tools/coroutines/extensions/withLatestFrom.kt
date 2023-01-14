package by.tigre.music.player.tools.coroutines.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun <A, B : Any, R : Any> Flow<A>.withLatestFrom(other: Flow<B>, transform: suspend (A, B) -> R): Flow<R> = flow {
    coroutineScope {
        val latestB = AtomicReference<B>()
        val outerScope = this
        launch {
            try {
                other.collect { latestB.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        collect { a: A ->
            latestB.get()?.let { b -> emit(transform(a, b)) }
        }
    }
}

fun <A, B, C : Any, R : Any> Flow<A>.withLatestFrom(
    otherB: Flow<B>,
    otherC: Flow<C>,
    transform: suspend (A, B, C) -> R
): Flow<R> = flow {
    coroutineScope {
        val latestB = AtomicReference<B>()
        val latestC = AtomicReference<C>()
        val outerScope = this
        launch {
            try {
                otherB.collect { latestB.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        launch {
            try {
                otherC.collect { latestC.set(it) }
            } catch (e: CancellationException) {
                outerScope.cancel(e) // cancel outer scope on cancellation exception, too
            }
        }
        collect { a: A ->
            val b = latestB.get()
            val c = latestC.get()
            if (b != null && c != null) {
                emit(transform(a, b, c))
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <A : Any, B : Any, C : Any> Flow<A>.withLatestFrom(otherB: Flow<B>, otherC: Flow<C>): Flow<Triple<A, B, C>> =
    withLatestFrom(otherB, otherC) { a, b, c -> Triple(a, b, c) }


@Suppress("NOTHING_TO_INLINE")
inline fun <A : Any, B : Any> Flow<A>.withLatestFrom(other: Flow<B>): Flow<Pair<A, B>> =
    withLatestFrom(other) { a, b -> a to b }

@Suppress("NOTHING_TO_INLINE")
inline fun <A : Any, B : Any> Flow<A>.filterByLatestFrom(
    other: Flow<B>, crossinline predicate: (A, B) -> Boolean
): Flow<A> =
    withLatestFrom(other) { a, b -> a to b }
        .filter { (a, b) -> predicate(a, b) }
        .map { (a, _) -> a }
