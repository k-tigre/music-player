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

    fun loadBasePlanIds(): Map<String, String> {
        val raw = preferences.getString(BASE_PLANS_KEY, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return raw.split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val parts = entry.split(KEY_VALUE_SEPARATOR, limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
    }

    fun saveBasePlanIds(basePlanIds: Map<String, String>) {
        val encoded = basePlanIds.entries.joinToString(ENTRY_SEPARATOR) { (productId, basePlanId) ->
            "$productId$KEY_VALUE_SEPARATOR$basePlanId"
        }
        preferences.edit().putString(BASE_PLANS_KEY, encoded).apply()
    }

    fun rememberBasePlan(productId: String, basePlanId: String) {
        val updated = loadBasePlanIds().toMutableMap()
        updated[productId] = basePlanId
        saveBasePlanIds(updated)
    }

    fun retainBasePlansForProducts(ownedProductIds: Set<String>): Map<String, String> {
        val retained = loadBasePlanIds().filterKeys { it in ownedProductIds }
        saveBasePlanIds(retained)
        return retained
    }

    private companion object {
        const val PREFERENCES_NAME = "entitlements"
        const val TIER_KEY = "entitlements.tier"
        const val BASE_PLANS_KEY = "entitlements.base_plans"
        const val ENTRY_SEPARATOR = ";"
        const val KEY_VALUE_SEPARATOR = "="
    }
}
