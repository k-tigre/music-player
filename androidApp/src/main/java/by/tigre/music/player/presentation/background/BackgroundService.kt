package by.tigre.music.player.presentation.background

import android.content.Intent
import by.tigre.music.player.App
import by.tigre.music.player.MainActivity
import by.tigre.music.player.core.presentation.backgound_player.di.PlayerBackgroundDependency
import by.tigre.music.player.core.presentation.backgound_player.presentation.platform.PlaybackService

class BackgroundService : PlaybackService() {
    override fun onProviderMainIntent(): Intent = Intent(this, MainActivity::class.java)
    override fun onProviderDependency(): PlayerBackgroundDependency = (application as App).graph
}
