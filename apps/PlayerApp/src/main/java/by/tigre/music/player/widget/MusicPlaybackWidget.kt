package by.tigre.music.player.widget

import by.tigre.media.platform.background.widget.PlaybackWidgetProvider
import by.tigre.music.player.MainActivity
import by.tigre.music.player.presentation.background.BackgroundService

class MusicPlaybackWidget : PlaybackWidgetProvider() {
    override fun backgroundServiceClass() = BackgroundService::class.java
    override fun mainActivityClass() = MainActivity::class.java
}
