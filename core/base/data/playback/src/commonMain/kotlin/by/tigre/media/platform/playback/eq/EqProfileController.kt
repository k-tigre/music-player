package by.tigre.media.platform.playback.eq

import by.tigre.media.platform.playback.PlaybackEqualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Applies the best matching EQ profile when route or content changes.
 * Manual writes happen via [endEqSessionAndSaveIfDirty] after leaving the EQ screen.
 * Hardware EQ is always touched on Main (ExoPlayer / Android Equalizer requirement).
 */
class EqProfileController(
    private val scope: CoroutineScope,
    private val repository: EqProfileRepository,
    private val routeMonitor: AudioRouteMonitor,
    private val contentKeyProvider: EqContentKeyProvider,
    private val playbackEqualizer: PlaybackEqualizer,
    private val maxAutoProfiles: Int,
    private val maxTotalProfiles: Int = DEFAULT_MAX_TOTAL,
) {
    private val _lastResolve = MutableStateFlow(EqResolveResult(null, EqMatchLevel.None))
    val lastResolve: StateFlow<EqResolveResult> = _lastResolve.asStateFlow()

    val currentRoute: StateFlow<AudioRouteId> = routeMonitor.currentRoute
    val profiles: StateFlow<List<EqProfile>> = repository.profiles
    val bookId: StateFlow<Long?> = contentKeyProvider.bookId
    val folderKey: StateFlow<EqContentKey.Folder?> = contentKeyProvider.folderKey
    val albumId: StateFlow<Long?> = contentKeyProvider.albumId
    val artistKey: StateFlow<EqContentKey.Artist?> = contentKeyProvider.artistKey
    val contentKey: StateFlow<EqContentKey> = contentKeyProvider.contentKey

    private val _carryForwardNotices = MutableSharedFlow<EqCarryForwardNotice>(extraBufferCapacity = 1)
    val carryForwardNotices: SharedFlow<EqCarryForwardNotice> = _carryForwardNotices.asSharedFlow()

    private val _lifecycleEvents = MutableSharedFlow<EqProfileLifecycleEvent>(extraBufferCapacity = 16)
    val lifecycleEvents: SharedFlow<EqProfileLifecycleEvent> = _lifecycleEvents.asSharedFlow()

    /** Last resolve outcome used when opening the EQ screen (for correction analytics). */
    private val _activeOutcome = MutableStateFlow("none")
    val activeOutcome: StateFlow<String> = _activeOutcome.asStateFlow()
    private val _activeSource = MutableStateFlow<EqProfileSource?>(null)
    val activeSource: StateFlow<EqProfileSource?> = _activeSource.asStateFlow()

    private var eqSessionOpen = false
    private var sessionBaselineGains: List<Float> = emptyList()
    private var sessionBaselinePreset: Int = -1
    private var lastAppliedContent: EqContentKey = EqContentKey.None
    private var lastAppliedRouteKey: String? = null
    private var suppressResolve = false
    private var lastLifecycleEmitKey: String? = null

    init {
        scope.launch {
            repository.refresh()
            combine(
                combine(
                    repository.profiles,
                    routeMonitor.currentRoute,
                    contentKeyProvider.contentKey,
                    contentKeyProvider.folderKey,
                    contentKeyProvider.bookId,
                ) { profiles, route, content, folder, bookId ->
                    ResolvePartial(profiles, route, content, folder, bookId)
                },
                contentKeyProvider.artistKey,
                contentKeyProvider.albumId,
            ) { partial, artist, albumId ->
                ResolveInput(
                    profiles = partial.profiles,
                    route = partial.route,
                    content = partial.content,
                    folder = partial.folder,
                    bookId = partial.bookId,
                    artist = artist,
                    albumId = albumId,
                )
            }
                .distinctUntilChanged()
                .collect { input ->
                    if (suppressResolve) return@collect
                    withContext(Dispatchers.Main.immediate) {
                        applyResolve(input)
                    }
                }
        }
    }

    fun beginEqSession() {
        eqSessionOpen = true
        captureBaseline()
    }

    fun isSessionDirty(): Boolean {
        if (!eqSessionOpen) return false
        val gains = playbackEqualizer.bandGainDb.value
        val preset = playbackEqualizer.selectedPresetIndex.value
        if (preset != sessionBaselinePreset) return true
        if (gains.size != sessionBaselineGains.size) return true
        return gains.indices.any { gains[it] != sessionBaselineGains[it] }
    }

    fun discardEqSession() {
        eqSessionOpen = false
    }

    /**
     * Persist dirty EQ as a manual profile for the current content key.
     * @return false if limit blocked a new profile
     */
    suspend fun endEqSessionAndSaveIfDirty(): Boolean {
        if (!isSessionDirty()) {
            eqSessionOpen = false
            return true
        }
        eqSessionOpen = false
        return saveManualForCurrentContent()
    }

    suspend fun deleteProfile(id: Long) {
        repository.delete(id)
    }

    private fun captureBaseline() {
        sessionBaselineGains = playbackEqualizer.bandGainDb.value.toList()
        sessionBaselinePreset = playbackEqualizer.selectedPresetIndex.value
    }

    private suspend fun saveManualForCurrentContent(): Boolean {
        val content = contentKeyProvider.contentKey.value
        val ok = saveCurrent(content, EqProfileSource.Manual)
        if (ok) {
            maybeSeedDeviceDefault()
        }
        return ok
    }

    private suspend fun maybeSeedDeviceDefault() {
        val route = routeMonitor.currentRoute.value
        val hasDevice = repository.profiles.value.any {
            it.route.storageKey() == route.storageKey() && it.content is EqContentKey.None
        }
        if (!hasDevice) {
            saveCurrent(EqContentKey.None, EqProfileSource.Manual)
        }
    }

    private suspend fun saveCurrent(content: EqContentKey, source: EqProfileSource): Boolean {
        val gains = playbackEqualizer.bandGainDb.value
        val preset = playbackEqualizer.selectedPresetIndex.value
        val custom = playbackEqualizer.customPresetIndex.value
        val profile = EqProfile(
            id = 0,
            route = routeMonitor.currentRoute.value,
            content = content,
            presetIndex = if (custom >= 0 && preset >= custom) null else preset,
            gainsDb = gains,
            title = null,
            updatedAtMs = System.currentTimeMillis(),
            source = source,
        )
        return repository.save(profile, maxAuto = maxAutoProfiles, maxTotal = maxTotalProfiles)
    }

    private fun applyResolve(input: ResolveInput) {
        val result = resolveInput(input)
        val routeKey = input.route.storageKey()
        val contentChanged =
            lastAppliedRouteKey == routeKey &&
                lastAppliedContent != EqContentKey.None &&
                input.content != EqContentKey.None &&
                lastAppliedContent != input.content
        val exactHit = when (result.matchLevel) {
            EqMatchLevel.Book, EqMatchLevel.Folder, EqMatchLevel.Album, EqMatchLevel.Artist -> true
            else -> false
        }

        when {
            exactHit && result.profile != null -> {
                _lastResolve.value = result
                applyProfile(result.profile)
                emitLifecycle(
                    outcome = "exact",
                    matchLevel = result.matchLevel,
                    source = result.profile.source,
                    route = input.route,
                    content = input.content,
                )
                afterApply(input)
            }
            contentChanged -> {
                // Keep hardware gains; seed auto for the new content; soft toast.
                _lastResolve.value = EqResolveResult(null, EqMatchLevel.None)
                emitLifecycle(
                    outcome = "carry",
                    matchLevel = EqMatchLevel.None,
                    source = EqProfileSource.Auto,
                    route = input.route,
                    content = input.content,
                )
                scope.launch {
                    suppressResolve = true
                    try {
                        saveCurrent(input.content, EqProfileSource.Auto)
                    } finally {
                        suppressResolve = false
                    }
                    _carryForwardNotices.emit(EqCarryForwardNotice)
                }
                afterApply(input)
                if (eqSessionOpen) captureBaseline()
            }
            result.profile != null && result.matchLevel == EqMatchLevel.Device -> {
                _lastResolve.value = result
                applyProfile(result.profile)
                val seeded = input.content !is EqContentKey.None
                emitLifecycle(
                    outcome = if (seeded) "device_seed" else "exact",
                    matchLevel = EqMatchLevel.Device,
                    source = result.profile.source,
                    route = input.route,
                    content = input.content,
                )
                if (seeded) {
                    scope.launch {
                        suppressResolve = true
                        try {
                            saveCurrent(input.content, EqProfileSource.Auto)
                        } finally {
                            suppressResolve = false
                        }
                    }
                }
                afterApply(input)
            }
            else -> {
                _lastResolve.value = EqResolveResult(null, EqMatchLevel.None)
                emitLifecycle(
                    outcome = "none",
                    matchLevel = EqMatchLevel.None,
                    source = null,
                    route = input.route,
                    content = input.content,
                )
                afterApply(input)
            }
        }
    }

    private fun emitLifecycle(
        outcome: String,
        matchLevel: EqMatchLevel,
        source: EqProfileSource?,
        route: AudioRouteId,
        content: EqContentKey,
    ) {
        val sourceName = source?.storageName.orEmpty()
        val key =
            listOf(
                route.storageKey(),
                content.kind.storageName,
                content.storageKey,
                outcome,
                sourceName,
            ).joinToString("|")
        if (key == lastLifecycleEmitKey) return
        lastLifecycleEmitKey = key
        _activeOutcome.value = outcome
        _activeSource.value = source
        _lifecycleEvents.tryEmit(
            EqProfileLifecycleEvent(
                outcome = outcome,
                matchLevel = matchLevel,
                source = source,
                routeKind = route.kind.name.lowercase(),
                contentKind = content.kind.storageName,
            ),
        )
    }

    private fun afterApply(input: ResolveInput) {
        lastAppliedContent = input.content
        lastAppliedRouteKey = input.route.storageKey()
        if (eqSessionOpen) captureBaseline()
    }

    private fun resolveInput(input: ResolveInput): EqResolveResult = when {
        input.bookId != null && input.folder != null -> {
            EqProfileResolver.resolveForBook(
                profiles = input.profiles,
                route = input.route,
                bookId = input.bookId,
                folderUri = input.folder.folderUri,
                subPath = input.folder.subPath,
            )
        }
        input.albumId != null && input.artist != null -> {
            EqProfileResolver.resolveForMusic(
                profiles = input.profiles,
                route = input.route,
                albumId = input.albumId,
                artistId = input.artist.artistId,
            )
        }
        input.content is EqContentKey.Folder -> {
            EqProfileResolver.resolve(input.profiles, input.route, input.content)
        }
        else -> {
            EqProfileResolver.resolve(input.profiles, input.route, input.content)
        }
    }

    private fun applyProfile(profile: EqProfile) {
        val firstCustom = playbackEqualizer.customPresetIndex.value
        val customCount = playbackEqualizer.customPresetCount.value
        val gains = profile.gainsDb
        if (gains.isNotEmpty() && firstCustom >= 0 && customCount > 0) {
            val selected = playbackEqualizer.selectedPresetIndex.value
            val targetCustom =
                if (selected >= firstCustom && selected < firstCustom + customCount) {
                    selected
                } else {
                    firstCustom
                }
            val centers = playbackEqualizer.bandCenterHz.value
            val aligned = if (centers.isEmpty()) {
                gains
            } else {
                EqGainInterpolation.alignOrRemapToUiBands(gains, centers.toFloatArray())
            }
            playbackEqualizer.selectPreset(targetCustom)
            aligned.forEachIndexed { index, gain ->
                playbackEqualizer.setBandGainDb(index, gain)
            }
        } else {
            val preset = profile.presetIndex ?: return
            if (preset >= 0) playbackEqualizer.selectPreset(preset)
        }
    }

    private data class ResolvePartial(
        val profiles: List<EqProfile>,
        val route: AudioRouteId,
        val content: EqContentKey,
        val folder: EqContentKey.Folder?,
        val bookId: Long?,
    )

    private data class ResolveInput(
        val profiles: List<EqProfile>,
        val route: AudioRouteId,
        val content: EqContentKey,
        val folder: EqContentKey.Folder?,
        val bookId: Long?,
        val artist: EqContentKey.Artist?,
        val albumId: Long?,
    )

    companion object {
        const val DEFAULT_MAX_TOTAL = 128
        const val MAX_AUTO_AUDIOBOOK = 5
        const val MAX_AUTO_MUSIC = 16
    }
}
