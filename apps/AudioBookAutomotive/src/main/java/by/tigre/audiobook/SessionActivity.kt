package by.tigre.audiobook

import android.app.Activity
import android.os.Bundle

/**
 * Minimal entry for [androidx.media3.session.MediaSession] session activity on AAOS (no phone UI).
 */
class SessionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
