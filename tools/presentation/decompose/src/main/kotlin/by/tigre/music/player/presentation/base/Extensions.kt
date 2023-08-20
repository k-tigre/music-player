package by.tigre.music.player.presentation.base

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigationSource
import com.arkivanov.decompose.router.pages.childPages
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

@OptIn(ExperimentalDecomposeApi::class)
fun <C : Parcelable, T : Any> BaseComponentContext.appChildPages(
    source: PagesNavigationSource<C>,
    initialPages: () -> Pages<C>,
    configurationClass: KClass<out C>,
    key: String = "DefaultChildPages",
    persistent: Boolean = true,
    handleBackButton: Boolean = false,
    childFactory: (configuration: C, BaseComponentContext) -> T,
): Value<ChildPages<C, T>> =
    childPages(
        source = source,
        configurationClass = configurationClass,
        initialPages = initialPages,
        key = key,
        persistent = persistent,
        handleBackButton = handleBackButton
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

@OptIn(ExperimentalDecomposeApi::class)
inline fun <reified C : Parcelable, T : Any> BaseComponentContext.appChildPages(
    source: PagesNavigationSource<C>,
    noinline initialPages: () -> Pages<C>,
    persistent: Boolean = true,
    handleBackButton: Boolean = false,
    noinline childFactory: (configuration: C, BaseComponentContext) -> T,
): Value<ChildPages<C, T>> =
    appChildPages(
        source = source,
        initialPages = initialPages,
        configurationClass = C::class,
        handleBackButton = handleBackButton,
        childFactory = childFactory,
        persistent = persistent,
    )

fun BaseComponentContext.appChildContext(key: String, lifecycle: Lifecycle? = null): BaseComponentContext =
    BaseComponentContextImpl(
        componentContext = childContext(key, lifecycle)
    )
