package by.tigre.music.player.logger

import java.io.PrintWriter
import java.io.StringWriter

@Suppress("NOTHING_TO_INLINE")
object Log {

    interface Logger {
        fun log(level: Level, vararg fields: Pair<String, String>)
    }

    enum class Level(val value: Int) {
        // Start from 2 to be in sync with android.util.Log.
        VERBOSE(2), DEBUG(3), INFO(4), WARN(5), ERROR(6), ASSERT(7)
    }

    object Field {
        const val TAG = "tag"
        const val MESSAGE = "message"
        const val TIMESTAMP = "timestamp"
        const val THREAD = "thread"
        const val STACKTRACE = "stacktrace"
    }

    var enabled: Boolean = false
        private set
    var level = Level.INFO
        private set
    private var loggers: Array<out Logger> = emptyArray()

    fun init(level: Level = Level.ASSERT, vararg loggers: Logger) {
        enabled = loggers.isNotEmpty()
        this.loggers = loggers
        this.level = level
    }

    inline fun v(message: () -> String) = log(Level.VERBOSE, message)
    inline fun v(tag: String, message: () -> String) = log(Level.VERBOSE, tag, message)
    inline fun v(tag: String, message: String) = log(Level.VERBOSE, Field.MESSAGE to message, Field.TAG to tag)

    inline fun d(message: () -> String) = log(Level.DEBUG, message)
    inline fun d(tag: String, message: () -> String) = log(Level.DEBUG, tag, message)
    inline fun d(tag: String, message: String) = log(Level.DEBUG, Field.MESSAGE to message, Field.TAG to tag)

    inline fun i(message: () -> String) = log(Level.INFO, message)
    inline fun i(tag: String, message: () -> String) = log(Level.INFO, tag, message)
    inline fun i(tag: String, message: String) = log(Level.INFO, Field.MESSAGE to message, Field.TAG to tag)
    inline fun i(throwable: Throwable, message: () -> String) = log(Level.INFO, throwable, message)

    inline fun w(message: () -> String) = log(Level.WARN, message)
    inline fun w(tag: String, message: () -> String) = log(Level.WARN, tag, message)
    inline fun w(throwable: Throwable, message: () -> String) = log(Level.WARN, throwable, message)
    inline fun w(throwable: Throwable, tag: String, message: () -> String) = log(Level.WARN, throwable, tag, message)

    inline fun e(message: () -> String) = log(Level.ERROR, message)
    inline fun e(tag: String, message: () -> String) = log(Level.ERROR, tag, message)
    inline fun e(throwable: Throwable, message: () -> String) = log(Level.ERROR, throwable, message)
    inline fun e(tag: String, message: String) = log(Level.ERROR, Field.MESSAGE to message, Field.TAG to tag)
    inline fun e(throwable: Throwable, message: String) = log(Level.ERROR, throwable) { message }

    fun log(level: Level, vararg fields: Pair<String, String>) {
        if (Log.level <= level && enabled) {
            val timestamp = System.currentTimeMillis()
            val thread = Thread.currentThread().name

            loggers.forEach { tree ->
                tree.log(level, *fields, Field.TIMESTAMP to timestamp.toString(), Field.THREAD to thread)
            }
        }
    }

    inline fun log(level: Level, message: () -> String) {
        if (Log.level <= level && enabled) {
            log(level, Field.MESSAGE to message())
        }
    }

    inline fun log(level: Level, tag: String, message: () -> String) {
        if (Log.level <= level && enabled) {
            log(level, Field.MESSAGE to message(), Field.TAG to tag)
        }
    }

    inline fun log(level: Level, throwable: Throwable, message: () -> String) {
        if (Log.level <= level && enabled) {
            val stacktrace = StringWriter().apply {
                PrintWriter(this).apply {
                    throwable.printStackTrace(this)
                    flush()
                }
            }.toString()

            log(level, Field.MESSAGE to message(), Field.STACKTRACE to stacktrace)
        }
    }

    inline fun log(level: Level, throwable: Throwable, tag: String, message: () -> String) {
        if (Log.level <= level && enabled) {
            val stacktrace = StringWriter().apply {
                PrintWriter(this).apply {
                    throwable.printStackTrace(this)
                    flush()
                }
            }.toString()

            log(level, Field.MESSAGE to message(), Field.STACKTRACE to stacktrace, Field.TAG to tag)
        }
    }
}
