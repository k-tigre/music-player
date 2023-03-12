package by.tigre.music.player.core.data.playback.impl

import by.tigre.music.player.core.data.entiry.playback.MediaItemWrapper
import by.tigre.music.player.core.data.playback.MediaItemWrapperProvider
import by.tigre.music.player.core.entiry.catalog.Song

internal class MediaItemWrapperProviderImpl : MediaItemWrapperProvider {
    override fun songToMediaItem(songItem: Song): MediaItemWrapper = MediaItemWrapper.Impl(songItem)
}
