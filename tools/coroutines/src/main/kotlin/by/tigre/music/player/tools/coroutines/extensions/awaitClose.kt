package by.tigre.music.player.tools.coroutines.extensions

import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun awaitClose(action: () -> Unit): Unit = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        action()
    }
}
