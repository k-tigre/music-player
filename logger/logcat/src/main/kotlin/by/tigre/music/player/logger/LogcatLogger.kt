package by.tigre.music.player.logger

import android.util.Log as AndroidLog

class LogcatLogger : Log.Logger {

    companion object {
        private const val CALL_STACK_INDEX = 3
        private const val MAX_LOG_LENGTH = 4000
    }

    override fun log(level: Log.Level, vararg fields: Pair<String, String>) {
        val fieldsMap = fields.toMap()

        val tag = fieldsMap[Log.Field.TAG] ?: stackTraceTag()
        val message = fieldsMap[Log.Field.MESSAGE] ?: fieldsMap.toString()
        val otherFields = fields.filter { (key, _) ->
            key != Log.Field.MESSAGE
                    && key != Log.Field.STACKTRACE
                    && key != Log.Field.TAG
                    && key != Log.Field.THREAD
                    && key != Log.Field.TIMESTAMP
        }.joinToString(
            separator = "\n",
            transform = { (key, value) -> "$key: $value" }
        )
        val stacktrace = fieldsMap[Log.Field.STACKTRACE]
        val thread = fieldsMap[Log.Field.THREAD]
        val messageWithThread = "[$thread] $message\n$otherFields${if (stacktrace == null) "" else "\n$stacktrace"}"

        if (messageWithThread.length < MAX_LOG_LENGTH) {
            androidLog(level.value, tag, messageWithThread)
        } else {
            val length = messageWithThread.length
            var start = 0
            while (start < length) {
                val end = (start + MAX_LOG_LENGTH).coerceAtMost(length)
                androidLog(level.value, tag, messageWithThread.substring(start, end))
                start = end
            }
        }
    }

    private fun androidLog(priority: Int, tag: String, message: String) {
        AndroidLog.println(priority, tag, message)
    }

    private fun stackTraceTag() = Throwable().stackTrace.let {
        if (it.size > CALL_STACK_INDEX) {
            it[CALL_STACK_INDEX].className
        } else {
            ""
        }
    }
}
