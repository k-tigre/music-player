package by.tigre.music.player.presentation.background

import android.content.Intent
import by.tigre.music.player.App
import by.tigre.music.player.MainActivity
import by.tigre.media.platform.background.di.PlayerBackgroundDependency
import by.tigre.media.platform.background.presentation.platform.PlaybackService
import by.tigre.music.player.widget.MusicPlaybackWidget

class BackgroundService : PlaybackService() {
    override fun onProviderMainIntent(): Intent = Intent(this, MainActivity::class.java)
    override fun onProviderDependency(): PlayerBackgroundDependency = (application as App).graph
    override fun playbackWidgetProviderClass() = MusicPlaybackWidget::class.java
}
