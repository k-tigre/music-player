package by.tigre.music.player.logger

class ConsoleLogger : Log.Logger {

    override fun log(level: Log.Level, vararg fields: Pair<String, String>) {
        val fieldsMap = fields.toMap()
        val tag = fieldsMap[Log.Field.TAG] ?: ""
        val message = fieldsMap[Log.Field.MESSAGE] ?: ""
        val thread = fieldsMap[Log.Field.THREAD] ?: ""
        val stacktrace = fieldsMap[Log.Field.STACKTRACE]

        val levelName = when (level) {
            Log.Level.VERBOSE -> "V"
            Log.Level.DEBUG -> "D"
            Log.Level.INFO -> "I"
            Log.Level.WARN -> "W"
            Log.Level.ERROR -> "E"
            Log.Level.ASSERT -> "A"
        }

        val line = buildString {
            append(levelName)
            if (tag.isNotEmpty()) append("/$tag")
            append(" [$thread] ")
            append(message)
        }

        if (level.value >= Log.Level.ERROR.value) {
            System.err.println(line)
        } else {
            println(line)
        }

        if (stacktrace != null) {
            if (level.value >= Log.Level.ERROR.value) {
                System.err.println(stacktrace)
            } else {
                println(stacktrace)
            }
        }
    }
}
