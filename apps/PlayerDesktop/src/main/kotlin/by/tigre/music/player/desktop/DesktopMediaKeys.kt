package by.tigre.music.player.desktop

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.logger.Log
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.SwingUtilities

/**
 * Global multimedia keys (play/pause, next, previous, stop) via JNativeHook so playback works
 * when the window is minimized or not focused.
 */
internal object DesktopMediaKeys {

    fun install(controller: BasePlaybackController): () -> Unit {
        suppressNativeHookLogNoise()

        val listener = object : NativeKeyListener {
            override fun nativeKeyPressed(e: NativeKeyEvent) {
                val action = when (e.keyCode) {
                    NativeKeyEvent.VC_MEDIA_PLAY -> MediaAction.TogglePlayPause
                    NativeKeyEvent.VC_MEDIA_STOP -> MediaAction.Stop
                    NativeKeyEvent.VC_MEDIA_NEXT -> MediaAction.Next
                    NativeKeyEvent.VC_MEDIA_PREVIOUS -> MediaAction.Prev
                    else -> null
                } ?: return

                SwingUtilities.invokeLater { dispatch(controller, action) }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent) = Unit
            override fun nativeKeyTyped(e: NativeKeyEvent) = Unit
        }

        var registeredHookHere = false
        var listenerAdded = false
        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook()
                registeredHookHere = true
            }
            GlobalScreen.addNativeKeyListener(listener)
            listenerAdded = true
        } catch (e: NativeHookException) {
            Log.w(e, "DesktopMediaKeys") { "Media keys unavailable: ${e.message}" }
            if (registeredHookHere) {
                runCatching { GlobalScreen.unregisterNativeHook() }
            }
            return { }
        }

        return {
            runCatching {
                if (listenerAdded) {
                    GlobalScreen.removeNativeKeyListener(listener)
                }
                if (registeredHookHere && GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.unregisterNativeHook()
                }
            }
        }
    }

    private fun dispatch(controller: BasePlaybackController, action: MediaAction) {
        when (action) {
            MediaAction.TogglePlayPause -> {
                if (controller.player.state.value == PlaybackPlayer.State.Playing) {
                    controller.pause()
                } else {
                    controller.resume()
                }
            }
            MediaAction.Stop -> controller.stop()
            MediaAction.Next -> controller.playNext()
            MediaAction.Prev -> controller.playPrev()
        }
    }

    private enum class MediaAction {
        TogglePlayPause,
        Stop,
        Next,
        Prev,
    }

    private fun suppressNativeHookLogNoise() {
        Logger.getLogger(GlobalScreen::class.java.getPackage().name).level = Level.WARNING
    }
}
