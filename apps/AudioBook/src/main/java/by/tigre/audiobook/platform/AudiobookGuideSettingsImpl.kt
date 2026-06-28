package by.tigre.audiobook.platform

import by.tigre.media.platform.preferences.Preferences

class AudiobookGuideSettingsImpl(
    private val preferences: Preferences,
) : AudiobookGuideSettings {

    override fun shouldShowGuide(): Boolean =
        !preferences.loadBoolean(PREF_GETTING_STARTED_DISMISSED, default = false)

    override fun markGuideShown() {
        preferences.saveBoolean(PREF_GETTING_STARTED_DISMISSED, value = true)
    }

    private companion object {
        const val PREF_GETTING_STARTED_DISMISSED = "audiobook_getting_started_dismissed"
    }
}
