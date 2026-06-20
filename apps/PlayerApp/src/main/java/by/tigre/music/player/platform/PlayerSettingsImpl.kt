package by.tigre.music.player.platform

import android.content.Context
import by.tigre.media.platform.preferences.Preferences

class PlayerSettingsImpl(
    private val context: Context,
    private val preferences: Preferences,
) : PlayerSettings {

    override fun shouldShowPrompt(): Boolean {
        if (preferences.loadBoolean(PREF_PROMPTED_DEFAULT_PLAYER, default = false)) return false
        if (DefaultMusicPlayerRole.isHeld(context)) return false
        return true
    }

    override fun markPromptShown() {
        preferences.saveBoolean(PREF_PROMPTED_DEFAULT_PLAYER, value = true)
    }

    private companion object {
        const val PREF_PROMPTED_DEFAULT_PLAYER = "prompted_default_player"
    }
}
