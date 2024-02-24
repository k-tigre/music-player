package by.tigre.music.player.presentation.base

import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigationSource
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigationSource
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.parcelable.Parcelable
import kotlin.reflect.KClass

fun <C : Parcelable, T : Any> BaseComponentContext.appChildStack(
    source: StackNavigationSource<C>,
    initialStack: () -> List<C>,
    configurationClass: KClass<out C>,
    key: String = "DefaultStack",
    handleBackButton: Boolean = false,
    childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildStack<C, T>> =
    childStack(
        source = source,
        initialStack = initialStack,
        configurationClass = configurationClass,
        key = key,
        handleBackButton = handleBackButton
    ) { configuration, componentContext ->
        childFactory(
            configuration,
            BaseComponentContextImpl(
                componentContext = componentContext
            )
        )
    }

fun <C : Parcelable, T : Any> BaseComponentContext.appChildSlotI(
    source: SlotNavigationSource<C>,
    initialConfiguration: () -> C? = { null },
    configurationClass: KClass<out C>,
    key: String = "DefaultChildSlot",
    handleBackButton: Boolean = false,
    persistent: Boolean = true,
    childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildSlot<C, T>> =
    childSlot(
        source = source,
        initialConfiguration = initialConfiguration,
        configurationClass = configurationClass,
        key = key,
        handleBackButton = handleBackButton,
        persistent = persistent,
    ) { configuration, componentContext ->
        childFactory(
            configuration,
            BaseComponentContextImpl(
                componentContext = componentContext
            )
        )
    }

inline fun <reified C : Parcelable, T : Any> BaseComponentContext.appChildStack(
    source: StackNavigationSource<C>,
    noinline initialStack: () -> List<C>,
    key: String = "DefaultStack",
    handleBackButton: Boolean = false,
    noinline childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildStack<C, T>> =
    appChildStack(
        source = source,
        initialStack = initialStack,
        configurationClass = C::class,
        key = key,
        handleBackButton = handleBackButton,
        childFactory = childFactory,
    )

inline fun <reified C : Parcelable, T : Any> BaseComponentContext.appChildSlot(
    source: SlotNavigationSource<C>,
    noinline initialStack: () -> C? = { null },
    key: String = "DefaultChildSlot",
    handleBackButton: Boolean = false,
    persistent: Boolean = true,
    noinline childFactory: (configuration: C, BaseComponentContext) -> T
): Value<ChildSlot<C, T>> =
    appChildSlotI(
        source = source,
        initialConfiguration = initialStack,
        configurationClass = C::class,
        key = key,
        handleBackButton = handleBackButton,
        childFactory = childFactory,
        persistent = persistent
    )

fun BaseComponentContext.appChildContext(key: String, lifecycle: Lifecycle? = null): BaseComponentContext =
    BaseComponentContextImpl(
        componentContext = childContext(key, lifecycle)
    )
