package by.tigre.audiobook.settings

object InAppReviewRemoteConfig {
    const val KEY = "audiobook_in_app_review_enabled"

    fun defaultsMap(): Map<String, Any> = mapOf(KEY to true)
}
