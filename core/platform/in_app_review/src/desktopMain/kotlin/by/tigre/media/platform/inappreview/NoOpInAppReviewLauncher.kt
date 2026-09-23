package by.tigre.media.platform.inappreview

actual class ActivityRef

class NoOpInAppReviewLauncher : InAppReviewLauncher {
    override suspend fun launch(activity: ActivityRef): Boolean = false
}
