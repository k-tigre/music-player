package by.tigre.media.platform.entitlements

import android.content.Context

class EntitlementsCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): Tier = preferences
        .getString(TIER_KEY, null)
        ?.let { cachedTier -> Tier.entries.firstOrNull { it.name == cachedTier } }
        ?: Tier.Free

    fun save(tier: Tier) {
        preferences.edit().putString(TIER_KEY, tier.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "entitlements"
        const val TIER_KEY = "entitlements.tier"
    }
}
