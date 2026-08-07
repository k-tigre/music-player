package by.tigre.media.platform.playback.eq.impl

import app.cash.sqldelight.db.SqlDriver
import by.tigre.media.platform.playback.eq.AudioRouteId
import by.tigre.media.platform.playback.eq.EqContentKey
import by.tigre.media.platform.playback.eq.EqProfile
import by.tigre.media.platform.playback.eq.EqProfileRepository
import by.tigre.media.platform.playback.eq.db.DatabaseEqProfiles
import eq.EqProfileRow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EqProfileRepositoryImpl(
    private val database: DatabaseEqProfiles,
) : EqProfileRepository {

    private val mutex = Mutex()
    private val _profiles = MutableStateFlow<List<EqProfile>>(emptyList())
    override val profiles: StateFlow<List<EqProfile>> = _profiles.asStateFlow()

    override suspend fun refresh() = mutex.withLock {
        _profiles.value = database.eqProfileQueries.selectAll().executeAsList().map { it.toDomain() }
    }

    override suspend fun save(profile: EqProfile, maxProfiles: Int): Boolean = mutex.withLock {
        val routeKey = profile.route.storageKey()
        val kind = profile.content.kind.storageName
        val contentKey = profile.content.storageKey
        val existing = database.eqProfileQueries
            .selectByRouteContent(routeKey, kind, contentKey)
            .executeAsList()
            .firstOrNull()
        val gains = profile.gainsDb.joinToString(",") { it.toString() }
        val preset = profile.presetIndex?.toLong()
        val now = profile.updatedAtMs
        if (existing != null) {
            database.eqProfileQueries.updateById(
                preset_index = preset,
                gains_csv = gains,
                title = profile.title,
                updated_at_ms = now,
                id = existing.id,
            )
        } else {
            val count = database.eqProfileQueries.countAll().executeAsList().firstOrNull() ?: 0L
            if (count >= maxProfiles) return@withLock false
            database.eqProfileQueries.insert(
                route_key = routeKey,
                content_kind = kind,
                content_key = contentKey,
                preset_index = preset,
                gains_csv = gains,
                title = profile.title,
                updated_at_ms = now,
            )
        }
        _profiles.value = database.eqProfileQueries.selectAll().executeAsList().map { it.toDomain() }
        true
    }

    override suspend fun delete(id: Long) = mutex.withLock {
        database.eqProfileQueries.deleteById(id)
        _profiles.value = database.eqProfileQueries.selectAll().executeAsList().map { it.toDomain() }
    }

    companion object {
        fun createDatabase(driver: SqlDriver): DatabaseEqProfiles = DatabaseEqProfiles(driver)
    }
}

private fun EqProfileRow.toDomain(): EqProfile {
    val kind = EqContentKey.Kind.fromStorage(content_kind)
    return EqProfile(
        id = id,
        route = AudioRouteId.parse(route_key),
        content = EqContentKey.fromStorage(kind, content_key),
        presetIndex = preset_index?.toInt(),
        gainsDb = gains_csv.split(',').mapNotNull { it.trim().toFloatOrNull() },
        title = title,
        updatedAtMs = updated_at_ms,
    )
}
