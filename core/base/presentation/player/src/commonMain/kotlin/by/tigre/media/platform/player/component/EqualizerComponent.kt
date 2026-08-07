package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqMatchLevel
import by.tigre.media.platform.playback.eq.EqProfileController
import by.tigre.media.platform.playback.eq.EqSaveTarget
import by.tigre.media.platform.player.di.PlayerDependency
import by.tigre.media.platform.tools.analytics.common.CommonEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface EqualizerComponent {
    val playbackEqualizer: PlaybackEqualizer
    val appPlaybackVolume: AppPlaybackVolume?
    val profileStatus: StateFlow<ProfileStatus>
    val needsSetupPrompt: StateFlow<Boolean>
    val preferredSaveTarget: StateFlow<EqSaveTarget>

    fun dismissSetupPrompt()
    fun savePreferred()
    fun close()

    data class ProfileStatus(
        val route: AudioRouteId,
        val matchLevel: EqMatchLevel,
        val hasProfile: Boolean,
        /** Last folder path segment when match is folder / for copy. */
        val folderName: String?,
    )

    class Impl(
        private val dependency: PlayerDependency,
        private val onClose: () -> Unit,
    ) : EqualizerComponent {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        override val playbackEqualizer: PlaybackEqualizer = dependency.playbackEqualizer
        override val appPlaybackVolume: AppPlaybackVolume? = dependency.appPlaybackVolume
        private val controller: EqProfileController = dependency.eqProfileController
        private val maxProfiles = dependency.eqProfileMaxCount
        private val analytics = dependency.eventAnalytics
        private val includeContent = dependency.eqSupportsContentProfiles

        override val profileStatus: StateFlow<ProfileStatus> =
            combine(
                controller.lastResolve,
                controller.currentRoute,
                controller.folderKey,
            ) { resolve, route, folder ->
                ProfileStatus(
                    route = route,
                    matchLevel = resolve.matchLevel,
                    hasProfile = resolve.profile != null,
                    folderName = folder?.subPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        ?: folder?.subPath?.takeIf { it.isNotBlank() },
                )
            }.stateIn(
                scope,
                SharingStarted.WhileSubscribed(5_000),
                ProfileStatus(
                    route = controller.currentRoute.value,
                    matchLevel = controller.lastResolve.value.matchLevel,
                    hasProfile = controller.lastResolve.value.profile != null,
                    folderName = null,
                ),
            )

        override val needsSetupPrompt: StateFlow<Boolean> = controller.needsSetupPrompt

        override val preferredSaveTarget: StateFlow<EqSaveTarget> =
            combine(controller.bookId, controller.folderKey) { _, _ ->
                controller.preferredSaveTarget(includeContent)
            }.stateIn(
                scope,
                SharingStarted.WhileSubscribed(5_000),
                controller.preferredSaveTarget(includeContent),
            )

        override fun dismissSetupPrompt() {
            analytics.trackEvent(
                CommonEvents.Action.EqProfilePromptDismissed(
                    route = controller.currentRoute.value.storageKey(),
                ),
            )
            controller.dismissSetupPrompt()
        }

        override fun savePreferred() {
            val target = preferredSaveTarget.value
            if (!dependency.hasEqDeviceProfilesAccess()) {
                analytics.trackEvent(
                    CommonEvents.Action.FeatureGateBlocked(
                        feature = "EqDeviceProfiles",
                        reason = "requires_purchase",
                        source = "equalizer_save",
                    ),
                )
                dependency.requestEqDeviceProfilesPaywall()
                return
            }
            scope.launch {
                val ok = controller.saveAs(target, maxProfiles)
                if (ok) {
                    analytics.trackEvent(
                        CommonEvents.Action.EqProfileSaved(
                            target = target.name.lowercase(),
                            route = controller.currentRoute.value.storageKey(),
                        ),
                    )
                } else {
                    analytics.trackEvent(
                        CommonEvents.Action.EqProfileSaveFailed(
                            reason = "limit_or_missing_content",
                            target = target.name.lowercase(),
                        ),
                    )
                }
            }
        }

        override fun close() = onClose()
    }
}
