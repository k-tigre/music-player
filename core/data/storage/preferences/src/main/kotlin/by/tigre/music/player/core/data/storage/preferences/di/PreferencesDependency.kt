package by.tigre.music.player.core.data.storage.preferences.di

import android.content.Context
import by.tigre.music.player.core.data.storage.preferences.Preferences

interface PreferencesDependency {
    val preferences: Preferences

    class Impl(context: Context): PreferencesDependency{
        override val preferences: Preferences by lazy { Preferences.Impl(context, "main_settings") }
    }
}