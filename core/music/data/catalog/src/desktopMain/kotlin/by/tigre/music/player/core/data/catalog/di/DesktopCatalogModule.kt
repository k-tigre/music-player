package by.tigre.music.player.core.data.catalog.di

import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.catalog.desktop.DesktopCatalogSourceImpl
import java.io.File

class DesktopCatalogModule(dbDir: File) : CatalogModule {

    private val sourceImpl: DesktopCatalogSourceImpl by lazy { DesktopCatalogSourceImpl(dbDir) }

    override val catalogSource: CatalogSource by lazy { sourceImpl }

    suspend fun addFolder(folder: File) = sourceImpl.addFolder(folder)
}
