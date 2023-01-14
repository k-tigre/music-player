package by.tigre.music.player.android.presentation.background

import by.tigre.music.player.android.presentation.background.BackgroundView.Action
import by.tigre.music.player.tools.coroutines.extensions.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

interface BackgroundPresenter : CoroutineScope {

    class Impl(private val view: BackgroundView) : BackgroundPresenter {

        private val job = SupervisorJob()

        override val coroutineContext: CoroutineContext = Dispatchers.Main + job

        init {
            view.onCreate()

            launch {
                awaitClose { view.onDestroy() }
            }

            launch {
                view.notificationAction
                    .collect { action ->
                        when (action) {
                            Action.RestoreScreen -> {} // TODO
                            Action.Stop -> {
                                view.onDestroy()
                            }
                        }
                    }
            }
        }
    }
}
