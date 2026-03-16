package by.tigre.music.player.presentation.base

import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

fun <C : Any, T : Any> BaseComponentContext.appChildStack(
    source: StackNavigation<C>,
    serializer: KSerializer<C>?,
    initialStack: () -> List<C>,
    key: String = "DefaultStack",
    handleBackButton: Boolean = false,
    childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildStack<C, T>> =
    childStack(
        source = source,
        serializer = serializer,
        initialStack = initialStack,
        key = key,
        handleBackButton = handleBackButton
    ) { configuration, componentContext ->
        childFactory(
            configuration,
            BaseComponentContextImpl(componentContext = componentContext)
        )
    }

inline fun <reified C : Any, T : Any> BaseComponentContext.appChildStack(
    source: StackNavigation<C>,
    noinline initialStack: () -> List<C>,
    key: String = "DefaultStack",
    handleBackButton: Boolean = false,
    noinline childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildStack<C, T>> =
    appChildStack(
        source = source,
        serializer = serializer<C>(),
        initialStack = initialStack,
        key = key,
        handleBackButton = handleBackButton,
        childFactory = childFactory,
    )

fun <C : Any, T : Any> BaseComponentContext.appChildSlot(
    source: SlotNavigation<C>,
    serializer: KSerializer<C>?,
    initialConfiguration: () -> C? = { null },
    key: String = "DefaultChildSlot",
    handleBackButton: Boolean = false,
    childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildSlot<C, T>> =
    childSlot(
        source = source,
        serializer = serializer,
        initialConfiguration = initialConfiguration,
        key = key,
        handleBackButton = handleBackButton,
    ) { configuration, componentContext ->
        childFactory(
            configuration,
            BaseComponentContextImpl(componentContext = componentContext)
        )
    }

fun BaseComponentContext.appChildContext(key: String, lifecycle: Lifecycle? = null): BaseComponentContext =
    BaseComponentContextImpl(componentContext = childContext(key, lifecycle))
