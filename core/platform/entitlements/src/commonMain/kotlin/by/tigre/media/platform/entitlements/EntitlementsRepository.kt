package by.tigre.media.platform.entitlements

import kotlinx.coroutines.flow.StateFlow

interface EntitlementsRepository {
    val tier: StateFlow<Tier>

    fun has(feature: Feature): Boolean

    fun playlistLimit(): Int

    fun continueListeningLimit(): Int

    suspend fun refresh()

    suspend fun restore()
}
