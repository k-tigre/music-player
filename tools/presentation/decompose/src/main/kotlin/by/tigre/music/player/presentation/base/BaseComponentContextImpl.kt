package by.tigre.music.player.presentation.base

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class BaseComponentContextImpl(
    componentContext: ComponentContext,
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()
) : BaseComponentContext, ComponentContext by componentContext {

    init {
        lifecycle.doOnDestroy { cancel() }
    }
}
