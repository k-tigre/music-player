package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import by.tigre.music.player.core.data.entiry.playback.SongItem

interface MediaItemWrapperProvider {
    fun songToMediaItem(songItem: SongItem): MediaItemWrapper

    class Impl() : MediaItemWrapperProvider {
        override fun songToMediaItem(songItem: SongItem): MediaItemWrapper {
            return MediaItemWrapper.Impl(songItem)
        }
    }
}