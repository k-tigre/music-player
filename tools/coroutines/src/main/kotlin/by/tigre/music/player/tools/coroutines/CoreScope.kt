package by.tigre.music.player.tools.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface CoreScope : CoroutineScope {
    class Impl : CoreScope, CoroutineScope by CoroutineScope(Dispatchers.Default)
}
