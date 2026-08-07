package by.tigre.media.platform.playback.eq

import by.tigre.media.platform.playback.PlaybackEqualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Applies the best matching EQ profile when route or content changes.
 * Writes to DB only via [saveCurrent] / repository delete — never on slider moves.
 */
class EqProfileController(
    private val scope: CoroutineScope,
    private val repository: EqProfileRepository,
    private val routeMonitor: AudioRouteMonitor,
    private val contentKeyProvider: EqContentKeyProvider,
    private val playbackEqualizer: PlaybackEqualizer,
    private val suggestEnabled: () -> Boolean = { true },
) {
    private val _lastResolve = MutableStateFlow(EqResolveResult(null, EqMatchLevel.None))
    val lastResolve: StateFlow<EqResolveResult> = _lastResolve.asStateFlow()

    val currentRoute: StateFlow<AudioRouteId> = routeMonitor.currentRoute

    private val _needsSetupPrompt = MutableStateFlow(false)
    val needsSetupPrompt: StateFlow<Boolean> = _needsSetupPrompt.asStateFlow()

    private var dismissedPair: String? = null

    init {
        scope.launch {
            repository.refresh()
            combine(
                repository.profiles,
                routeMonitor.currentRoute,
                contentKeyProvider.contentKey,
                contentKeyProvider.folderKey,
                contentKeyProvider.bookId,
            ) { profiles, route, content, folder, bookId ->
                ResolveInput(profiles, route, content, folder, bookId)
            }
                .distinctUntilChanged()
                .collect { input ->
                    applyResolve(input)
                }
        }
    }

    fun dismissSetupPrompt() {
        val route = routeMonitor.currentRoute.value
        dismissedPair = promptKey(route, contentKeyProvider.contentKey.value)
        _needsSetupPrompt.value = false
    }

    /**
     * Persist current EQ bands for [content] on the active route.
     * @return false if profile limit reached for a new key
     */
    suspend fun saveCurrent(
        content: EqContentKey,
        maxProfiles: Int,
        title: String? = null,
    ): Boolean {
        val gains = playbackEqualizer.bandGainDb.value
        val preset = playbackEqualizer.selectedPresetIndex.value
        val custom = playbackEqualizer.customPresetIndex.value
        val profile = EqProfile(
            id = 0,
            route = routeMonitor.currentRoute.value,
            content = content,
            presetIndex = if (custom >= 0 && preset == custom) null else preset,
            gainsDb = gains,
            title = title,
            updatedAtMs = System.currentTimeMillis(),
        )
        val ok = repository.save(profile, maxProfiles)
        if (ok) {
            dismissedPair = null
            _needsSetupPrompt.value = false
        }
        return ok
    }

    /** Default save target: existing match content, else device-only. */
    suspend fun saveForActiveMatch(maxProfiles: Int): Boolean {
        val content = _lastResolve.value.profile?.content ?: EqContentKey.None
        return saveCurrent(content, maxProfiles)
    }

    private fun applyResolve(input: ResolveInput) {
        val result = when {
            input.bookId != null && input.folder != null -> {
                EqProfileResolver.resolveForBook(
                    profiles = input.profiles,
                    route = input.route,
                    bookId = input.bookId,
                    folderUri = input.folder.folderUri,
                    subPath = input.folder.subPath,
                )
            }
            input.content is EqContentKey.Folder -> {
                EqProfileResolver.resolve(input.profiles, input.route, input.content)
            }
            else -> {
                EqProfileResolver.resolve(input.profiles, input.route, input.content)
            }
        }
        _lastResolve.value = result
        val profile = result.profile
        if (profile != null) {
            _needsSetupPrompt.value = false
            applyProfile(profile)
        } else {
            val key = promptKey(input.route, input.content)
            _needsSetupPrompt.value = suggestEnabled() && dismissedPair != key
        }
    }

    private fun applyProfile(profile: EqProfile) {
        val customIndex = playbackEqualizer.customPresetIndex.value
        val gains = profile.gainsDb
        if (gains.isNotEmpty() && customIndex >= 0) {
            playbackEqualizer.selectPreset(customIndex)
            gains.forEachIndexed { index, gain ->
                playbackEqualizer.setBandGainDb(index, gain)
            }
        } else {
            val preset = profile.presetIndex ?: return
            if (preset >= 0) playbackEqualizer.selectPreset(preset)
        }
    }

    private fun promptKey(route: AudioRouteId, content: EqContentKey): String =
        "${route.storageKey()}|${content.kind.storageName}|${content.storageKey}"

    private data class ResolveInput(
        val profiles: List<EqProfile>,
        val route: AudioRouteId,
        val content: EqContentKey,
        val folder: EqContentKey.Folder?,
        val bookId: Long?,
    )
}
