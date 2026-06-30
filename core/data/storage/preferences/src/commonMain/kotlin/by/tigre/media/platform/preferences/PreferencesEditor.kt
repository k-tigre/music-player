package by.tigre.media.platform.preferences

class PreferencesEditor internal constructor() {
    internal val ops = mutableListOf<Op>()

    fun put(key: String, value: Boolean) {
        ops += Op.PutBoolean(key, value)
    }

    fun put(key: String, value: String?) {
        ops += Op.PutString(key, value)
    }

    fun put(key: String, value: Int) {
        ops += Op.PutInt(key, value)
    }

    fun put(key: String, value: Long) {
        ops += Op.PutLong(key, value)
    }

    fun remove(key: String) {
        ops += Op.Remove(key)
    }

    internal sealed interface Op {
        data class PutBoolean(val key: String, val value: Boolean) : Op
        data class PutString(val key: String, val value: String?) : Op
        data class PutInt(val key: String, val value: Int) : Op
        data class PutLong(val key: String, val value: Long) : Op
        data class Remove(val key: String) : Op
    }
}
