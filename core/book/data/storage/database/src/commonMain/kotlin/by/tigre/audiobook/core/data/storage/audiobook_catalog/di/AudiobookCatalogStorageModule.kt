package by.tigre.audiobook.core.data.storage.audiobook_catalog.di

import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage

interface AudiobookCatalogStorageModule {
    val audiobookCatalogStorage: AudiobookCatalogStorage
    val audiobookPlaybackStorage: AudiobookPlaybackStorage
}
