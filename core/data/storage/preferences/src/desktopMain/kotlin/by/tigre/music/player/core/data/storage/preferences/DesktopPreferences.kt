package by.tigre.music.player.core.data.storage.preferences

class DesktopPreferences(name: String) : Preferences {
    private val prefs = java.util.prefs.Preferences.userRoot().node(name)

    override fun saveBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        prefs.flush()
    }

    override fun saveBooleans(vararg values: Pair<String, Boolean>) {
        values.forEach { (key, value) -> prefs.putBoolean(key, value) }
        prefs.flush()
    }

    override fun loadBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun saveString(key: String, value: String?) {
        if (value != null) {
            prefs.put(key, value)
        } else {
            prefs.remove(key)
        }
        prefs.flush()
    }

    override fun loadString(key: String, default: String?): String? = prefs.get(key, default)

    override fun saveInt(key: String, value: Int?) {
        if (value != null) {
            prefs.putInt(key, value)
        } else {
            prefs.remove(key)
        }
        prefs.flush()
    }

    override fun loadInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun saveLong(key: String, value: Long?) {
        if (value != null) {
            prefs.putLong(key, value)
        } else {
            prefs.remove(key)
        }
        prefs.flush()
    }

    override fun loadLong(key: String, default: Long): Long = prefs.getLong(key, default)
}
