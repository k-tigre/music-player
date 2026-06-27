package by.tigre.audiobook.widget

import by.tigre.audiobook.MainActivity
import by.tigre.audiobook.presentation.background.BackgroundService
import by.tigre.media.platform.background.widget.PlaybackWidgetProvider

class AudiobookPlaybackWidget : PlaybackWidgetProvider() {
    override fun backgroundServiceClass() = BackgroundService::class.java
    override fun mainActivityClass() = MainActivity::class.java
}
