package by.tigre.music.player.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import by.tigre.music.player.android.presentation.background.BackgroundService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.play).setOnClickListener {
            this.startService(Intent(this, BackgroundService::class.java))
        }

        findViewById<View>(R.id.stop).setOnClickListener {
            this.stopService(Intent(this, BackgroundService::class.java))
        }
    }
}
