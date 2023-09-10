package by.tigre.music.player.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsLogger : Log.Logger {
    override fun log(level: Log.Level, vararg fields: Pair<String, String>) {
        if (level >= Log.Level.INFO) {
            val message = fields
                .filter { (key, _) ->
                    key != Log.Field.TIMESTAMP && key != Log.Field.STACKTRACE
                }
                .joinToString(
                    separator = ", ",
                    prefix = "$level; "
                ) { (key, value) -> "$key:$value" }
            FirebaseCrashlytics.getInstance().log(message)
        }
    }
}
