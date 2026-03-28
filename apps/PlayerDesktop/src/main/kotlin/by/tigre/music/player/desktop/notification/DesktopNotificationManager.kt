package by.tigre.music.player.desktop.notification

import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

class DesktopNotificationManager(
    private val controller: BasePlaybackController,
    private val appIcon: BufferedImage
) {
    private val os = System.getProperty("os.name") ?: ""
    private val isMac     = os.contains("mac",     ignoreCase = true)
    private val isWindows = os.contains("windows", ignoreCase = true)
    private val isLinux   = os.contains("linux",   ignoreCase = true)

    private val macNowPlaying: MacOsNowPlaying? = if (isMac)     MacOsNowPlaying(controller) else null
    private val windowsSmtc:   WindowsSmtc?     = if (isWindows) WindowsSmtc(controller)    else null
    private val linuxMpris:    LinuxMpris?       = if (isLinux)   LinuxMpris(controller)     else null

    // Overlay state — fallback when native integration is unavailable
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var trayIcon: TrayIcon? = null

    private val _overlayVisible = MutableStateFlow(false)
    val overlayVisible: StateFlow<Boolean> = _overlayVisible.asStateFlow()

    private val _overlayKey = MutableStateFlow(0)
    val overlayKey: StateFlow<Int> = _overlayKey.asStateFlow()

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    fun start() {
        setupTrayIcon()
        when {
            isMac     -> macNowPlaying?.start()
            isWindows -> windowsSmtc?.start() ?: startOverlay()
            isLinux   -> linuxMpris?.start()  ?: startOverlay()
            else      -> startOverlay()
        }
    }

    fun dismissOverlay() {
        _overlayVisible.value = false
    }

    fun stop() {
        macNowPlaying?.stop()
        windowsSmtc?.stop()
        linuxMpris?.stop()
        scope.cancel()
        trayIcon?.let { runCatching { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        Log.i(TAG) { "stopped" }
    }

    private fun startOverlay() {
        scope.launch {
            var lastItem: PlayerItem? = null
            controller.currentItem.collect { item ->
                if (item != null && item != lastItem) {
                    lastItem = item
                    _currentItem.value = item
                    _overlayKey.update { it + 1 }
                    _overlayVisible.value = true
                }
            }
        }
    }

    private fun setupTrayIcon() {
        if (!SystemTray.isSupported()) return
        runCatching {
            val tray = SystemTray.getSystemTray()
            val size = tray.trayIconSize
            val scaled = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB).also { dst ->
                val g = dst.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(appIcon, 0, 0, size.width, size.height, null)
                g.dispose()
            }
            val icon = TrayIcon(scaled, "Music Player")
            tray.add(icon)
            trayIcon = icon
        }
    }

    companion object {
        private const val TAG = "DesktopNotificationManager"
    }
}
