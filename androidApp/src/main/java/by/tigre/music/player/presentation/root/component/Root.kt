package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.core.presentation.catalog.component.RootCatalogComponent
import by.tigre.music.player.core.presentation.catalog.component.SmallPlayerComponent
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerComponentProvider
import by.tigre.music.player.presentation.base.BaseComponentContext
import by.tigre.music.player.presentation.base.appChildContext

interface Root {

    val playerComponent: SmallPlayerComponent
    val catalogComponent: RootCatalogComponent

    class Impl(
        context: BaseComponentContext,
        catalogComponentProvider: CatalogComponentProvider,
        playerComponentProvider: PlayerComponentProvider
    ) : Root, BaseComponentContext by context {

        override val playerComponent: SmallPlayerComponent by lazy {
            playerComponentProvider.createSmallPlayerComponent(appChildContext("player"))
        }

        override val catalogComponent: RootCatalogComponent by lazy {
            catalogComponentProvider.createRootCatalogComponent(appChildContext("catalog"))
        }

        init {
            // TODO handle catalog|playlist|player views
        }

    }
}
