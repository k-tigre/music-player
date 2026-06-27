package by.tigre.audiobook.presentation.background

import android.content.Intent
import by.tigre.audiobook.App
import by.tigre.audiobook.MainActivity
import by.tigre.audiobook.widget.AudiobookPlaybackWidget
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.background.presentation.platform.PlaybackService

class BackgroundService : PlaybackService() {
    override fun onProviderMainIntent(): Intent = Intent(this, MainActivity::class.java)
    override fun onProviderDependency(): PlayerBackgroundDependency = (application as App).graph
    override fun playbackWidgetProviderClass() = AudiobookPlaybackWidget::class.java
}
