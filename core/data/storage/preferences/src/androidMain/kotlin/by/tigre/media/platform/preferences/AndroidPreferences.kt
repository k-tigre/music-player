package by.tigre.media.platform.preferences

import android.content.Context
import android.content.SharedPreferences

class AndroidPreferences(context: Context, name: String) : Preferences {
    private val preference = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun saveBoolean(key: String, value: Boolean) {
        save { put(key, value) }
    }

    override fun saveBooleans(vararg values: Pair<String, Boolean>) {
        save { values.forEach { (key, value) -> put(key, value) } }
    }

    override fun loadBoolean(key: String, default: Boolean): Boolean = preference.getBoolean(key, default)

    override fun saveString(key: String, value: String?) {
        save { put(key, value) }
    }

    override fun loadString(key: String, default: String?): String? = preference.getString(key, default)

    override fun saveInt(key: String, value: Int?) {
        if (value != null) {
            save { put(key, value) }
        } else {
            save { remove(key) }
        }
    }

    override fun loadInt(key: String, default: Int): Int = preference.getInt(key, default)

    override fun saveLong(key: String, value: Long?) {
        if (value != null) {
            save { put(key, value) }
        } else {
            save { remove(key) }
        }
    }

    override fun loadLong(key: String, default: Long): Long = preference.getLong(key, default)

    override fun save(block: PreferencesEditor.() -> Unit) {
        val editor = PreferencesEditor().apply(block)
        if (editor.ops.isEmpty()) return
        val edit = preference.edit()
        editor.ops.forEach { applyOp(edit, it) }
        edit.apply()
    }

    private fun applyOp(edit: SharedPreferences.Editor, op: PreferencesEditor.Op) {
        when (op) {
            is PreferencesEditor.Op.PutBoolean -> edit.putBoolean(op.key, op.value)
            is PreferencesEditor.Op.PutString -> edit.putString(op.key, op.value)
            is PreferencesEditor.Op.PutInt -> edit.putInt(op.key, op.value)
            is PreferencesEditor.Op.PutLong -> edit.putLong(op.key, op.value)
            is PreferencesEditor.Op.Remove -> edit.remove(op.key)
        }
    }
}
