package by.tigre.music.player.core.data.catalog

/**
 * Resolves MediaStore volume names without Android APIs so selection rules can
 * be unit-tested. Devices throw
 * `IllegalArgumentException: Volume external_primary not found` when a query
 * targets a volume that is not currently mounted.
 */
internal object MediaStoreVolumes {
    const val EXTERNAL = "external"
    const val EXTERNAL_PRIMARY = "external_primary"

    /**
     * Volume for read queries. Returns null when nothing is mounted.
     *
     * Prefer the synthetic [EXTERNAL] view when primary storage is available
     * (covers primary + secondary). If primary is missing but another volume
     * exists, query that volume by name — synthetic `external` can still crash
     * when `external_primary` is absent.
     */
    fun resolveForRead(availableVolumes: Set<String>): String? {
        if (availableVolumes.isEmpty()) return null
        return if (EXTERNAL_PRIMARY in availableVolumes) {
            EXTERNAL
        } else {
            availableVolumes.first()
        }
    }

    /** Writable volume for delete/insert; primary only. */
    fun resolveForWrite(availableVolumes: Set<String>): String? =
        EXTERNAL_PRIMARY.takeIf { it in availableVolumes }
}
