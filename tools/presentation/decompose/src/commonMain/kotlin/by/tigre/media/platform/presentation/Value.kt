package by.tigre.media.platform.presentation

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T : Any> Value<T>.toFlow(): Flow<T> = callbackFlow {
    val observer: (T) -> Unit = { value: T -> trySend(value) }
    val cancellation = subscribe(observer)
    awaitClose { cancellation.cancel() }
}
