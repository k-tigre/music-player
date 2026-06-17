package by.tigre.media.platform.preferences.di

import by.tigre.media.platform.preferences.DesktopPreferences
import by.tigre.media.platform.preferences.Preferences

class DesktopPreferencesModule(name: String = "music_player") : PreferencesModule {
    override val preferences: Preferences by lazy { DesktopPreferences(name) }
}
