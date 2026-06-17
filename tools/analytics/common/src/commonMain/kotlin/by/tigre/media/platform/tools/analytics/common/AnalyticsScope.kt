package by.tigre.media.platform.tools.analytics.common

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AnalyticsScope(vararg val apps: AnalyticsApp)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AnalyticsDoc(val description: String)
