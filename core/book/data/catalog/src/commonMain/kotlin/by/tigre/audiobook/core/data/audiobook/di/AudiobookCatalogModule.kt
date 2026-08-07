package by.tigre.audiobook.core.data.audiobook.di

import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook.spaces.LibrarySpaceRepository

interface AudiobookCatalogModule {
    val audiobookCatalogSource: AudiobookCatalogSource
    val librarySpaceRepository: LibrarySpaceRepository
}
