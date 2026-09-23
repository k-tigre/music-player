package by.tigre.music.player.settings

object InAppReviewRemoteConfig {
    const val KEY = "music_in_app_review_enabled"

    fun defaultsMap(): Map<String, Any> = mapOf(KEY to true)
}
