package by.tigre.music.player.core.data.storage.preferences

interface Preferences {
    fun saveBoolean(key: String, value: Boolean)
    fun saveBooleans(vararg values: Pair<String, Boolean>)
    fun loadBoolean(key: String, default: Boolean): Boolean

    fun saveString(key: String, value: String?)
    fun loadString(key: String, default: String?): String?

    fun saveInt(key: String, value: Int?)
    fun loadInt(key: String, default: Int): Int

    fun saveLong(key: String, value: Long?)
    fun loadLong(key: String, default: Long): Long
}
