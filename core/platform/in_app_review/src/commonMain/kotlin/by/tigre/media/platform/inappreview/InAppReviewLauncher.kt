package by.tigre.media.platform.inappreview

expect class ActivityRef

interface InAppReviewLauncher {
    /**
     * @return true if Play review flow launch was started (counts as an attempt).
     */
    suspend fun launch(activity: ActivityRef): Boolean
}
