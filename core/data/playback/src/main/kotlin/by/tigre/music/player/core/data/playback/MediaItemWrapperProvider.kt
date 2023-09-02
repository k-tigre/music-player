package by.tigre.music.player.core.data.playback

import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import by.tigre.music.player.core.entiry.catalog.Song

internal interface MediaItemWrapperProvider {
    fun songToMediaItem(songItem: Song): MediaItemWrapper
}
