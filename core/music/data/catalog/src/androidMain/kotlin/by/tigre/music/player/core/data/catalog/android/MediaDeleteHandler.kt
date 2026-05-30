package by.tigre.music.player.core.data.catalog.android

import android.net.Uri

interface MediaDeleteHandler {
    suspend fun delete(uris: List<Uri>): Boolean
}

object MediaDeleteHandlerRegistry {
    private var handler: MediaDeleteHandler? = null

    fun register(handler: MediaDeleteHandler) {
        this.handler = handler
    }

    fun unregister() {
        handler = null
    }

    suspend fun delete(uris: List<Uri>): Boolean {
        val active = handler
        return if (active != null) {
            active.delete(uris)
        } else {
            false
        }
    }
}
