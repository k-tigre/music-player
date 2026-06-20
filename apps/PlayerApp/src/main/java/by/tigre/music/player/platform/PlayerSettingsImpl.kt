package by.tigre.music.player.platform

import by.tigre.media.platform.preferences.Preferences

class PlayerSettingsImpl(
    private val preferences: Preferences,
) : PlayerSettings {

    override fun shouldShowPrompt(): Boolean =
        preferences.loadBoolean(PREF_PROMPTED_DEFAULT_PLAYER, default = false).not()

    override fun markPromptShown() {
        preferences.saveBoolean(PREF_PROMPTED_DEFAULT_PLAYER, value = true)
    }

    private companion object {
        const val PREF_PROMPTED_DEFAULT_PLAYER = "prompted_default_player"
    }
}
