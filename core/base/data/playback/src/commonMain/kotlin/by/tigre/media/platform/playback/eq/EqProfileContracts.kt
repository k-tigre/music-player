package by.tigre.media.platform.playback.eq

import kotlinx.coroutines.flow.StateFlow

interface EqProfileRepository {
    /** In-memory snapshot; updated after load / write. Resolver must use this, not hit DB. */
    val profiles: StateFlow<List<EqProfile>>

    suspend fun refresh()

    /**
     * Insert or replace by (route, content).
     * - Existing key: always updated (including auto → manual).
     * - New [EqProfileSource.Auto]: if [maxAuto] autos already exist, replaces the oldest auto.
     * - New [EqProfileSource.Manual]: fails only if [maxTotal] would be exceeded.
     */
    suspend fun save(profile: EqProfile, maxAuto: Int, maxTotal: Int): Boolean

    suspend fun delete(id: Long)
}

interface EqContentKeyProvider {
    val contentKey: StateFlow<EqContentKey>
    /** When playing a book, also expose folder for fallback resolve. */
    val folderKey: StateFlow<EqContentKey.Folder?>
    val bookId: StateFlow<Long?>
    /** When playing music, artist for fallback resolve. */
    val artistKey: StateFlow<EqContentKey.Artist?>
    val albumId: StateFlow<Long?>
}

interface AudioRouteMonitor {
    val currentRoute: StateFlow<AudioRouteId>
}
