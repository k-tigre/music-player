package by.tigre.music.player.android.extension

import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun awaitClose(action: () -> Unit): Unit = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { action() }
}
