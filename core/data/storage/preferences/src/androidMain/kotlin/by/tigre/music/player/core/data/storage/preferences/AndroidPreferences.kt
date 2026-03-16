package by.tigre.music.player.core.data.storage.preferences

import android.content.Context

class AndroidPreferences(context: Context, name: String) : Preferences {
    private val preference = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun saveBoolean(key: String, value: Boolean) {
        preference.edit().putBoolean(key, value).apply()
    }

    override fun saveBooleans(vararg values: Pair<String, Boolean>) {
        val editor = preference.edit()
        values.forEach { (key, value) -> editor.putBoolean(key, value) }
        editor.apply()
    }

    override fun loadBoolean(key: String, default: Boolean): Boolean = preference.getBoolean(key, default)

    override fun saveString(key: String, value: String?) {
        preference.edit().putString(key, value).apply()
    }

    override fun loadString(key: String, default: String?): String? = preference.getString(key, default)

    override fun saveInt(key: String, value: Int?) {
        preference.edit().run {
            if (value != null) {
                putInt(key, value)
            } else {
                remove(key)
            }
        }.apply()
    }

    override fun loadInt(key: String, default: Int): Int = preference.getInt(key, default)

    override fun saveLong(key: String, value: Long?) {
        preference.edit().run {
            if (value != null) {
                putLong(key, value)
            } else {
                remove(key)
            }
        }.apply()
    }

    override fun loadLong(key: String, default: Long): Long = preference.getLong(key, default)
}
