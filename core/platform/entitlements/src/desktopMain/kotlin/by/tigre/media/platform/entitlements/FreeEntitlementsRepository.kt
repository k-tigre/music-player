package by.tigre.media.platform.entitlements

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FreeEntitlementsRepository : EntitlementsRepository {
    override val tier: StateFlow<Tier> = MutableStateFlow(Tier.Free)

    override fun has(feature: Feature): Boolean = tier.value.includes(feature.minTier())

    override fun playlistLimit(): Int = EntitlementLimits.defaultPlaylistLimit(tier.value)

    override fun continueListeningLimit(): Int =
        EntitlementLimits.defaultContinueListeningLimit(tier.value)

    override suspend fun refresh() = Unit

    override suspend fun restore() = Unit
}
