package by.tigre.media.platform.background.car

/**
 * Catalog tree for Android Auto / Android for Cars media browser.
 * Implemented per app (music vs audiobook).
 */
interface CarMediaLibrary {

    /** Children of [parentId]; use [CarMediaIds.ROOT] for top-level tabs. */
    suspend fun getChildren(parentId: String): List<CarBrowseItem>

    /** Start playback for a single browsable/playable item. */
    fun playMediaId(mediaId: String)

    /** Replace queue / play a list (e.g. album or queue tab). */
    fun playMediaIds(mediaIds: List<String>) {}

    /** Resolve a single item for [onGetItem]; return null if unknown. */
    suspend fun getBrowseItem(mediaId: String): CarBrowseItem? = null
}
