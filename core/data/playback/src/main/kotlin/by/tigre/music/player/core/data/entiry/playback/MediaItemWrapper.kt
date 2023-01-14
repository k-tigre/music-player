package by.tigre.music.player.core.data.entiry.playback

interface MediaItemWrapper {
    val item: Any

    class Impl(override val item: Any): MediaItemWrapper // TODO connect to player
}
