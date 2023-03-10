package by.tigre.music.player.presentation.root.component

import by.tigre.music.player.presentation.base.BaseComponentContext

interface Root {


    class Impl(
        context: BaseComponentContext,
    ) : Root, BaseComponentContext by context {


        init {
            // TODO handle catalog|playlist|player views
        }

    }
}
