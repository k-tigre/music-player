package by.tigre.audiobook.core.data.audiobook.spaces

import by.tigre.media.platform.preferences.Preferences

class LibrarySpacePreferences(
    private val preferences: Preferences,
) {
    fun loadActiveSpaceId(): Long? =
        preferences.loadLong(KEY_ACTIVE_SPACE_ID, NO_ID).takeIf { it != NO_ID }

    fun saveActiveSpaceId(id: Long) {
        preferences.saveLong(KEY_ACTIVE_SPACE_ID, id)
    }

    private companion object {
        const val KEY_ACTIVE_SPACE_ID = "active_library_space_id"
        const val NO_ID = -1L
    }
}
