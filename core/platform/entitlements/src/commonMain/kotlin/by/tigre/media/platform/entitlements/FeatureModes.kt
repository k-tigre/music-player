package by.tigre.media.platform.entitlements

/**
 * Parses a flat JSON object of string keys to string values.
 * Unknown structure / invalid JSON → empty map (callers default modes to [FeatureMode.On]).
 */
fun parseFlatStringJsonObject(json: String): Map<String, String> {
    val trimmed = json.trim()
    if (trimmed.isEmpty() || trimmed == "{}") return emptyMap()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return emptyMap()

    val body = trimmed.substring(1, trimmed.lastIndex).trim()
    if (body.isEmpty()) return emptyMap()

    val result = linkedMapOf<String, String>()
    var i = 0
    while (i < body.length) {
        while (i < body.length && (body[i].isWhitespace() || body[i] == ',')) i++
        if (i >= body.length) break
        val key = readJsonString(body, i) ?: return emptyMap()
        i = key.endIndex
        while (i < body.length && body[i].isWhitespace()) i++
        if (i >= body.length || body[i] != ':') return emptyMap()
        i++
        while (i < body.length && body[i].isWhitespace()) i++
        val value = readJsonString(body, i) ?: return emptyMap()
        i = value.endIndex
        result[key.value] = value.value
    }
    return result
}

fun parseFeatureModes(json: String): Map<Feature, FeatureMode> =
    parseFlatStringJsonObject(json).mapNotNull { (key, value) ->
        FeatureModesRemoteConfig.featureFromJsonKey(key)?.let { feature ->
            feature to FeatureMode.parse(value)
        }
    }.toMap()

fun parseUnlockInstallationIds(csv: String): Set<String> =
    csv.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

fun resolveFeatureAccess(
    feature: Feature,
    tier: Tier,
    mode: FeatureMode,
    unlocked: Boolean,
): FeatureAccess {
    if (unlocked || mode == FeatureMode.On) return FeatureAccess.Allowed
    if (mode == FeatureMode.Off) return FeatureAccess.Unavailable
    return if (tier.includes(feature.minTier())) {
        FeatureAccess.Allowed
    } else {
        FeatureAccess.RequiresPurchase
    }
}

fun modeOrDefault(modes: Map<Feature, FeatureMode>, feature: Feature): FeatureMode =
    modes[feature] ?: FeatureMode.On

/**
 * Resolution for a single feature mode after Installation-ID overrides:
 * 1. unlock allowlist → [FeatureMode.On]
 * 2. force-paid allowlist → [FeatureMode.Paid]
 * 3. Remote Config JSON / default on
 */
fun effectiveFeatureMode(
    feature: Feature,
    modes: Map<Feature, FeatureMode>,
    unlocked: Boolean,
    forcePaid: Boolean,
): FeatureMode = when {
    unlocked -> FeatureMode.On
    forcePaid -> FeatureMode.Paid
    else -> modeOrDefault(modes, feature)
}

enum class FeatureAccess {
    Allowed,
    RequiresPurchase,
    Unavailable,
}

private data class JsonStringToken(val value: String, val endIndex: Int)

private fun readJsonString(source: String, start: Int): JsonStringToken? {
    if (start >= source.length || source[start] != '"') return null
    val builder = StringBuilder()
    var i = start + 1
    while (i < source.length) {
        when (val c = source[i]) {
            '\\' -> {
                if (i + 1 >= source.length) return null
                builder.append(source[i + 1])
                i += 2
            }
            '"' -> return JsonStringToken(builder.toString(), i + 1)
            else -> {
                builder.append(c)
                i++
            }
        }
    }
    return null
}
