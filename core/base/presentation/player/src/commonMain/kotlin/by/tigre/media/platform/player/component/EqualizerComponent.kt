package by.tigre.media.platform.player.component

import by.tigre.media.platform.playback.AppPlaybackVolume
import by.tigre.media.platform.playback.PlaybackEqualizer
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqContentKey
import by.tigre.media.platform.playback.eq.EqMatchLevel
import by.tigre.media.platform.playback.eq.EqProfile
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
    val suggestEnabled: StateFlow<Boolean>
    val savedProfiles: StateFlow<List<SavedProfileRow>>
    val saveTargets: StateFlow<List<EqSaveTarget>>

    fun dismissSetupPrompt()
    fun setSuggestEnabled(enabled: Boolean)
    fun saveAs(target: EqSaveTarget)
    fun deleteProfile(id: Long)
    fun close()

    data class ProfileStatus(
        val route: AudioRouteId,
        val matchLevel: EqMatchLevel,
        val hasProfile: Boolean,
    )

    data class SavedProfileRow(
        val id: Long,
        val routeLabel: String,
        val contentLabel: String,
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

        override val profileStatus: StateFlow<ProfileStatus> =
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
        override val suggestEnabled: StateFlow<Boolean> = controller.suggestEnabled

        override val savedProfiles: StateFlow<List<SavedProfileRow>> =
            combine(controller.profiles, controller.currentRoute) { profiles, _ ->
                profiles.map { it.toRow() }
            }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

        override val saveTargets: StateFlow<List<EqSaveTarget>> =
            combine(controller.bookId, controller.folderKey) { _, _ ->
                controller.availableSaveTargets(dependency.eqSupportsContentProfiles)
            }.stateIn(
                scope,
                SharingStarted.WhileSubscribed(5_000),
                controller.availableSaveTargets(dependency.eqSupportsContentProfiles),
            )

        override fun dismissSetupPrompt() {
            analytics.trackEvent(
                CommonEvents.Action.EqProfilePromptDismissed(
                    route = controller.currentRoute.value.storageKey(),
                ),
            )
            controller.dismissSetupPrompt()
        }

        override fun setSuggestEnabled(enabled: Boolean) {
            controller.setSuggestEnabled(enabled)
        }

        override fun saveAs(target: EqSaveTarget) {
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

        override fun deleteProfile(id: Long) {
            scope.launch {
                controller.deleteProfile(id)
                analytics.trackEvent(CommonEvents.Action.EqProfileDeleted)
            }
        }

        override fun close() = onClose()

        private fun EqProfile.toRow(): SavedProfileRow {
            val contentLabel = when (val c = content) {
                EqContentKey.None -> "device"
                is EqContentKey.Book -> "book:${c.bookId}"
                is EqContentKey.Folder -> {
                    val path = c.subPath.ifBlank { "/" }
                    "folder:$path"
                }
            }
            return SavedProfileRow(
                id = id,
                routeLabel = route.storageKey(),
                contentLabel = contentLabel,
            )
        }
    }
}
