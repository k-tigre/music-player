package by.tigre.media.platform.preferences

class DesktopPreferences(name: String) : Preferences {
    private val prefs = java.util.prefs.Preferences.userRoot().node(name)

    override fun saveBoolean(key: String, value: Boolean) {
        save { put(key, value) }
    }

    override fun saveBooleans(vararg values: Pair<String, Boolean>) {
        save { values.forEach { (key, value) -> put(key, value) } }
    }

    override fun loadBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun saveString(key: String, value: String?) {
        save { put(key, value) }
    }

    override fun loadString(key: String, default: String?): String? = prefs.get(key, default)

    override fun saveInt(key: String, value: Int?) {
        if (value != null) {
            save { put(key, value) }
        } else {
            save { remove(key) }
        }
    }

    override fun loadInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun saveLong(key: String, value: Long?) {
        if (value != null) {
            save { put(key, value) }
        } else {
            save { remove(key) }
        }
    }

    override fun loadLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun save(block: PreferencesEditor.() -> Unit) {
        val editor = PreferencesEditor().apply(block)
        if (editor.ops.isEmpty()) return
        editor.ops.forEach { applyOp(it) }
        prefs.flush()
    }

    private fun applyOp(op: PreferencesEditor.Op) {
        when (op) {
            is PreferencesEditor.Op.PutBoolean -> prefs.putBoolean(op.key, op.value)
            is PreferencesEditor.Op.PutString -> {
                if (op.value != null) {
                    prefs.put(op.key, op.value)
                } else {
                    prefs.remove(op.key)
                }
            }
            is PreferencesEditor.Op.PutInt -> prefs.putInt(op.key, op.value)
            is PreferencesEditor.Op.PutLong -> prefs.putLong(op.key, op.value)
            is PreferencesEditor.Op.Remove -> prefs.remove(op.key)
        }
    }
}
