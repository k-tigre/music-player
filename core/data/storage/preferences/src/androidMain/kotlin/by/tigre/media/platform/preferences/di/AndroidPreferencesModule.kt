package by.tigre.media.platform.preferences.di

import android.content.Context
import by.tigre.media.platform.preferences.AndroidPreferences
import by.tigre.media.platform.preferences.Preferences

class AndroidPreferencesModule(context: Context) : PreferencesModule {
    override val preferences: Preferences by lazy { AndroidPreferences(context, "main_settings") }
}
