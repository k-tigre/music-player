package by.tigre.music.player.tools.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface CoreDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher

    class Impl : CoreDispatchers {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.Default
    }
}
