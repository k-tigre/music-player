package by.tigre.media.platform.tools.coroutines

interface CoroutineModule {
    val scope: CoreScope

    class Impl : CoroutineModule {
        override val scope: CoreScope by lazy { CoreScope.Impl() }

    }
}
