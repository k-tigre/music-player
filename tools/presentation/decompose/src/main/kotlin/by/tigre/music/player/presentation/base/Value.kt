package by.tigre.music.player.presentation.base

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.observe
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun <T : Any> Value<T>.toFlow(
    lifeCycleOwner: LifecycleOwner,
    mode: ObserveLifecycleMode = ObserveLifecycleMode.CREATE_DESTROY
): Flow<T> = toStateFlow(lifeCycleOwner, mode)

fun <T : Any> Value<T>.toStateFlow(
    lifeCycleOwner: LifecycleOwner,
    mode: ObserveLifecycleMode = ObserveLifecycleMode.CREATE_DESTROY
): StateFlow<T> {
    val flow = MutableStateFlow(value)

    observe(lifeCycleOwner.lifecycle, mode, flow::tryEmit)

    return flow
}

fun <T : Any> StateFlow<T>.subscribeAsValue(scope: CoroutineScope): Value<T> = subscribeAsValue(value, scope)

fun <T : Any> Flow<T>.subscribeAsValue(startValue: T, scope: CoroutineScope): Value<T> {
    val value = MutableValue(startValue)

    scope.launch { collect { value.value = it } }

    return value
}
