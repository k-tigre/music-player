package by.tigre.audiobook.core.data.audiobook

/**
 * Result of a lightweight check whether the persisted SAF tree URI still works.
 * Does not scan for audio files — only tree open + root listing.
 */
sealed class FolderSourceAccessHealth {
    /** Root lists at least one child, or folder is empty and we have no indexed books for this source. */
    data object Ok : FolderSourceAccessHealth()

    /** `DocumentFile.fromTreeUri` returned null — persistable permission usually gone. */
    data object TreeUriUnavailable : FolderSourceAccessHealth()

    /** `DocumentFile.listFiles` returned null — often a provider / OEM issue while the URI is still stored. */
    data object CannotListContents : FolderSourceAccessHealth()

    /**
     * Root listing is empty but we still have books from this source in DB — treated as broken access,
     * not “user deleted all files”.
     */
    data object ListedButEmptyWithIndexedBooks : FolderSourceAccessHealth()
}
