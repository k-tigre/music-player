package by.tigre.music.player.tools.analytics.common

interface AnalyticsAction {
    val name: String
}

interface AnalyticsScreen {
    val name: String
    val skip: Boolean get() = false
}

interface WithPayload {
    val payload: Map<String, String>
}
