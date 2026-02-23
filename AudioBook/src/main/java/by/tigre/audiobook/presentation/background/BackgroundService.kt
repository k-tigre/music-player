package by.tigre.audiobook.presentation.background

import android.content.Intent
import by.tigre.audiobook.App
import by.tigre.audiobook.MainActivity
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.backgound_player.presentation.platform.PlaybackService

class BackgroundService : PlaybackService() {
    override fun onProviderMainIntent(): Intent = Intent(this, MainActivity::class.java)
    override fun onProviderDependency(): PlayerBackgroundDependency = (application as App).graph
}
