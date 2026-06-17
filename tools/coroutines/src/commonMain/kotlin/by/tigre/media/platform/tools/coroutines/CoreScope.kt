package by.tigre.media.platform.tools.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface CoreScope : CoroutineScope {
    class Impl : CoreScope, CoroutineScope by CoroutineScope(Dispatchers.Default)
}
