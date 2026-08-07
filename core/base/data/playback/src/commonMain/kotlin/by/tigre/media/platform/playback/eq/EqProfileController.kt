package by.tigre.media.platform.playback.eq

import by.tigre.media.platform.playback.PlaybackEqualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class EqSaveTarget {
    Device,
    Book,
    Folder,
}

/**
 * Applies the best matching EQ profile when route or content changes.
 * Writes to DB only via [saveAs] / repository delete — never on slider moves.
 */
class EqProfileController(
    private val scope: CoroutineScope,
    private val repository: EqProfileRepository,
    private val routeMonitor: AudioRouteMonitor,
    private val contentKeyProvider: EqContentKeyProvider,
    private val playbackEqualizer: PlaybackEqualizer,
    private val loadSuggestEnabled: () -> Boolean = { true },
    private val saveSuggestEnabled: (Boolean) -> Unit = {},
) {
    private val _lastResolve = MutableStateFlow(EqResolveResult(null, EqMatchLevel.None))
    val lastResolve: StateFlow<EqResolveResult> = _lastResolve.asStateFlow()

    val currentRoute: StateFlow<AudioRouteId> = routeMonitor.currentRoute
    val profiles: StateFlow<List<EqProfile>> = repository.profiles
    val bookId: StateFlow<Long?> = contentKeyProvider.bookId
    val folderKey: StateFlow<EqContentKey.Folder?> = contentKeyProvider.folderKey

    private val _suggestEnabled = MutableStateFlow(loadSuggestEnabled())
    val suggestEnabled: StateFlow<Boolean> = _suggestEnabled.asStateFlow()

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

    fun setSuggestEnabled(enabled: Boolean) {
        saveSuggestEnabled(enabled)
        _suggestEnabled.value = enabled
        if (!enabled) {
            _needsSetupPrompt.value = false
        }
    }

    fun dismissSetupPrompt() {
        val route = routeMonitor.currentRoute.value
        dismissedPair = promptKey(route, contentKeyProvider.contentKey.value)
        _needsSetupPrompt.value = false
    }

    fun availableSaveTargets(includeContent: Boolean): List<EqSaveTarget> = buildList {
        add(EqSaveTarget.Device)
        if (includeContent) {
            if (contentKeyProvider.bookId.value != null) add(EqSaveTarget.Book)
            if (contentKeyProvider.folderKey.value != null) add(EqSaveTarget.Folder)
        }
    }

    /**
     * Persist current EQ bands for [target] on the active route.
     * @return false if profile limit reached for a new key
     */
    suspend fun saveAs(
        target: EqSaveTarget,
        maxProfiles: Int,
        title: String? = null,
    ): Boolean {
        val content = when (target) {
            EqSaveTarget.Device -> EqContentKey.None
            EqSaveTarget.Book -> {
                val id = contentKeyProvider.bookId.value ?: return false
                EqContentKey.Book(id)
            }
            EqSaveTarget.Folder -> contentKeyProvider.folderKey.value ?: return false
        }
        return saveCurrent(content, maxProfiles, title)
    }

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

    /** Default: update active match, else device profile. */
    suspend fun saveForActiveMatch(maxProfiles: Int): Boolean {
        val content = _lastResolve.value.profile?.content ?: EqContentKey.None
        return saveCurrent(content, maxProfiles)
    }

    suspend fun deleteProfile(id: Long) {
        repository.delete(id)
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
            _needsSetupPrompt.value = _suggestEnabled.value && dismissedPair != key
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
