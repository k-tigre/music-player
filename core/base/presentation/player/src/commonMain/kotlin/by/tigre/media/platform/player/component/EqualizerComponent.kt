package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqContentKey
import by.tigre.media.platform.playback.eq.EqMatchLevel
import by.tigre.media.platform.playback.eq.EqProfileController
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

    fun close()

    data class ProfileStatus(
        val route: AudioRouteId,
        val matchLevel: EqMatchLevel,
        val hasProfile: Boolean,
        val contentKind: EqContentKey.Kind,
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
        private val analytics = dependency.eventAnalytics

        init {
            controller.beginEqSession()
        }

        override val profileStatus: StateFlow<ProfileStatus> =
            combine(
                controller.lastResolve,
                controller.currentRoute,
                controller.folderKey,
                controller.contentKey,
            ) { resolve, route, folder, content ->
                ProfileStatus(
                    route = route,
                    matchLevel = resolve.matchLevel,
                    hasProfile = resolve.profile != null,
                    contentKind = content.kind,
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
                    contentKind = controller.contentKey.value.kind,
                    folderName = null,
                ),
            )

        override fun close() {
            scope.launch {
                val dirty = controller.isSessionDirty()
                when {
                    !dirty -> controller.discardEqSession()
                    !dependency.hasEqDeviceProfilesAccess() -> {
                        controller.discardEqSession()
                        analytics.trackEvent(
                            CommonEvents.Action.FeatureGateBlocked(
                                feature = "EqDeviceProfiles",
                                reason = "requires_purchase",
                                source = "equalizer_autosave",
                            ),
                        )
                        dependency.requestEqDeviceProfilesPaywall()
                    }
                    else -> {
                        val ok = controller.endEqSessionAndSaveIfDirty()
                        if (ok) {
                            analytics.trackEvent(
                                CommonEvents.Action.EqProfileSaved(
                                    target = "manual_autosave",
                                    route = controller.currentRoute.value.storageKey(),
                                ),
                            )
                        } else {
                            analytics.trackEvent(
                                CommonEvents.Action.EqProfileSaveFailed(
                                    reason = "limit",
                                    target = "manual_autosave",
                                ),
                            )
                        }
                    }
                }
                onClose()
            }
        }
    }
}
