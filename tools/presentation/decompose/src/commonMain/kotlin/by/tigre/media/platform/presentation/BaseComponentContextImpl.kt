package by.tigre.media.platform.presentation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class BaseComponentContextImpl(
    componentContext: ComponentContext,
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
) : BaseComponentContext, ComponentContext by componentContext {

    init {
        lifecycle.doOnDestroy { cancel() }
    }
}
