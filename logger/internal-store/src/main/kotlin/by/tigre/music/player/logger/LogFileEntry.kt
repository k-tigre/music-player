package by.tigre.music.player.logger

internal class LogFileEntry(
    val level: Log.Level,
    val fields: Array<out Pair<String, String>>
) {
    val levelText by lazy {
        when (level) {
            Log.Level.ASSERT -> "ASSERT"
            Log.Level.DEBUG -> "DEBUG"
            Log.Level.VERBOSE -> "VERBOSE"
            Log.Level.INFO -> "INFO"
            Log.Level.WARN -> "WARN"
            Log.Level.ERROR -> "ERROR"
        }
    }
}