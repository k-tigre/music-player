package by.tigre.music.player.core.data.storage.preferences.di

import by.tigre.music.player.core.data.storage.preferences.DesktopPreferences
import by.tigre.music.player.core.data.storage.preferences.Preferences

class DesktopPreferencesModule(name: String = "music_player") : PreferencesModule {
    override val preferences: Preferences by lazy { DesktopPreferences(name) }
}
