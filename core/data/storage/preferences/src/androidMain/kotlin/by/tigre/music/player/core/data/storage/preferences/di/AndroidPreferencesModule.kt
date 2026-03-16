package by.tigre.music.player.core.data.storage.preferences.di

import android.content.Context
import by.tigre.music.player.core.data.storage.preferences.AndroidPreferences
import by.tigre.music.player.core.data.storage.preferences.Preferences

class AndroidPreferencesModule(context: Context) : PreferencesModule {
    override val preferences: Preferences by lazy { AndroidPreferences(context, "main_settings") }
}
