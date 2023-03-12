package by.tigre.music.player.core.data.entiry.playback

import by.tigre.music.player.core.entiry.catalog.Song

interface MediaItemWrapper {
    val item: Song

    class Impl(override val item: Song): MediaItemWrapper
}
