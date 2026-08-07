package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.StateFlow

interface EqProfileRepository {
    /** In-memory snapshot; updated after load / write. Resolver must use this, not hit DB. */
    val profiles: StateFlow<List<EqProfile>>

    suspend fun refresh()

    /**
     * Insert or replace by (route, content). Returns false if [maxProfiles] would be exceeded
     * for a new key (existing key replace always allowed).
     */
    suspend fun save(profile: EqProfile, maxProfiles: Int): Boolean

    suspend fun delete(id: Long)
}

interface EqContentKeyProvider {
    val contentKey: StateFlow<EqContentKey>
    /** When playing a book, also expose folder for fallback resolve. */
    val folderKey: StateFlow<EqContentKey.Folder?>
    val bookId: StateFlow<Long?>
}

interface AudioRouteMonitor {
    val currentRoute: StateFlow<AudioRouteId>
}
