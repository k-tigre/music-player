package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqMatchLevel
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.player.di.PlayerDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface EqualizerComponent {
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
    val profileStatus: StateFlow<ProfileStatus?>
    val needsSetupPrompt: StateFlow<Boolean>
    fun dismissSetupPrompt()
    fun saveForCurrentContext()
    fun close()

    data class ProfileStatus(
        val route: AudioRouteId,
        val matchLevel: EqMatchLevel,
        val hasProfile: Boolean,
    )

    class Impl(
        dependency: PlayerDependency,
        private val onClose: () -> Unit,
    ) : EqualizerComponent {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        override val playbackEqualizer: PlaybackEqualizer = dependency.playbackEqualizer
        override val appPlaybackVolume: AppPlaybackVolume? = dependency.appPlaybackVolume
        private val controller: EqProfileController = dependency.eqProfileController
        private val maxProfiles = dependency.eqProfileMaxCount

        override val profileStatus: StateFlow<ProfileStatus?> =
            combine(controller.lastResolve, controller.currentRoute) { resolve, route ->
                ProfileStatus(
                    route = route,
                    matchLevel = resolve.matchLevel,
                    hasProfile = resolve.profile != null,
                )
            }.stateIn(
                scope,
                SharingStarted.WhileSubscribed(5_000),
                ProfileStatus(
                    route = controller.currentRoute.value,
                    matchLevel = controller.lastResolve.value.matchLevel,
                    hasProfile = controller.lastResolve.value.profile != null,
                ),
            )

        override val needsSetupPrompt: StateFlow<Boolean> = controller.needsSetupPrompt

        override fun dismissSetupPrompt() {
            controller.dismissSetupPrompt()
        }

        override fun saveForCurrentContext() {
            scope.launch {
                controller.saveForActiveMatch(maxProfiles)
            }
        }

        override fun close() = onClose()
    }
}
