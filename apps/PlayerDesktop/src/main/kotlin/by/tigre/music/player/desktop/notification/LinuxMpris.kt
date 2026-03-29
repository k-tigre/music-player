package by.tigre.music.player.desktop.notification

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.logger.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.types.Variant
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties

/**
 * Linux MPRIS2 media player protocol over D-Bus.
 *
 * Exports an object at [OBJECT_PATH] on the session bus that implements:
 * - org.mpris.MediaPlayer2  (root interface — identity/capabilities)
 * - org.mpris.MediaPlayer2.Player  (playback control + metadata)
 * - org.freedesktop.DBus.Properties  (property access + PropertiesChanged signals)
 *
 * Tools like `playerctl` and desktop environment media widgets use this interface.
 */
class LinuxMpris(private val controller: BasePlaybackController) {

    // ── D-Bus interface definitions ────────────────────────────────────────────

    @DBusInterfaceName("org.mpris.MediaPlayer2")
    interface MediaPlayer2 : DBusInterface {
        fun Raise()
        fun Quit()
    }

    @DBusInterfaceName("org.mpris.MediaPlayer2.Player")
    interface MediaPlayer2Player : DBusInterface {
        fun Next()
        fun Previous()
        fun Pause()
        fun PlayPause()
        fun Stop()
        fun Play()
        fun Seek(Offset: Long)
        fun SetPosition(TrackId: DBusPath, Position: Long)
        fun OpenUri(Uri: String)
    }

    // ── Exported object ────────────────────────────────────────────────────────

    private inner class MprisObject : MediaPlayer2, MediaPlayer2Player, Properties {

        override fun getObjectPath(): String = OBJECT_PATH

        // MediaPlayer2
        override fun Raise() = Unit
        override fun Quit() = Unit

        // MediaPlayer2Player
        override fun Next() { controller.playNext() }
        override fun Previous() { controller.playPrev() }
        override fun Pause() { controller.pause() }
        override fun PlayPause() {
            if (controller.player.state.value == PlaybackPlayer.State.Playing) controller.pause()
            else controller.resume()
        }
        override fun Stop() { controller.pause() }
        override fun Play() { controller.resume() }
        override fun Seek(Offset: Long) {
            scope.launch {
                val pos = currentProgress?.position ?: return@launch
                controller.player.seekTo(pos + Offset / 1000L)
            }
        }
        override fun SetPosition(TrackId: DBusPath, Position: Long) {
            scope.launch { controller.player.seekTo(Position / 1000L) }
        }
        override fun OpenUri(Uri: String) = Unit

        // Properties
        @Suppress("UNCHECKED_CAST")
        override fun <A> Get(interface_name: String, property_name: String): A =
            (when (interface_name) {
                IFACE_ROOT   -> rootProperty(property_name)
                IFACE_PLAYER -> playerProperty(property_name)
                else         -> throw IllegalArgumentException("Unknown interface: $interface_name")
            }) as A

        override fun GetAll(interface_name: String): Map<String, Variant<*>> =
            when (interface_name) {
                IFACE_ROOT   -> rootProperties()
                IFACE_PLAYER -> playerProperties()
                else         -> emptyMap()
            }

        override fun <A> Set(interface_name: String, property_name: String, value: A) = Unit

        // ── Property builders ──────────────────────────────────────────────────

        private fun rootProperty(name: String): Any = when (name) {
            "Identity"           -> "Music Player"
            "CanQuit"            -> false
            "CanRaise"           -> false
            "HasTrackList"       -> false
            "SupportedUriSchemes"-> emptyList<String>()
            "SupportedMimeTypes" -> emptyList<String>()
            else                 -> throw IllegalArgumentException("Unknown property: $name")
        }

        private fun rootProperties(): Map<String, Variant<*>> = mapOf(
            "Identity"            to Variant("Music Player"),
            "CanQuit"             to Variant(false),
            "CanRaise"            to Variant(false),
            "HasTrackList"        to Variant(false),
            "SupportedUriSchemes" to Variant(emptyList<String>(), "as"),
            "SupportedMimeTypes"  to Variant(emptyList<String>(), "as"),
        )

        private fun playerProperty(name: String): Any = when (name) {
            "PlaybackStatus" -> currentPlaybackStatus
            "LoopStatus"     -> "None"
            "Rate"           -> 1.0
            "Shuffle"        -> false
            "Metadata"       -> currentMetadata
            "Volume"         -> 1.0
            "Position"       -> (currentProgress?.position ?: 0L) * 1000L
            "MinimumRate"    -> 1.0
            "MaximumRate"    -> 1.0
            "CanGoNext"      -> true
            "CanGoPrevious"  -> true
            "CanPlay"        -> true
            "CanPause"       -> true
            "CanSeek"        -> true
            "CanControl"     -> true
            else             -> throw IllegalArgumentException("Unknown property: $name")
        }

        fun playerProperties(): Map<String, Variant<*>> = mapOf(
            "PlaybackStatus" to Variant(currentPlaybackStatus),
            "LoopStatus"     to Variant("None"),
            "Rate"           to Variant(1.0),
            "Shuffle"        to Variant(false),
            "Metadata"       to Variant(currentMetadata, "a{sv}"),
            "Volume"         to Variant(1.0),
            "Position"       to Variant((currentProgress?.position ?: 0L) * 1000L, "x"),
            "MinimumRate"    to Variant(1.0),
            "MaximumRate"    to Variant(1.0),
            "CanGoNext"      to Variant(true),
            "CanGoPrevious"  to Variant(true),
            "CanPlay"        to Variant(true),
            "CanPause"       to Variant(true),
            "CanSeek"        to Variant(true),
            "CanControl"     to Variant(true),
        )
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var connection: org.freedesktop.dbus.connections.impl.DBusConnection? = null

    @Volatile private var currentPlaybackStatus = "Stopped"
    @Volatile private var currentMetadata: Map<String, Variant<*>> = emptyMap()
    @Volatile private var currentProgress: PlaybackPlayer.Progress? = null

    // ── Public API ─────────────────────────────────────────────────────────────

    fun start() {
        runCatching {
            val conn = DBusConnectionBuilder.forSessionBus()
                .withShared(false)
                .build()
            connection = conn
            conn.requestBusName(BUS_NAME)
            val obj = MprisObject()
            conn.exportObject(obj)
            scope.launch { observePlayback(conn) }
            Log.i(TAG) { "started" }
        }.onFailure { Log.e(TAG) { "start failed: $it" } }
    }

    fun stop() {
        scope.cancel()
        runCatching { connection?.releaseBusName(BUS_NAME) }
        runCatching { connection?.close() }
        connection = null
    }

    // ── Playback observation ───────────────────────────────────────────────────

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private suspend fun observePlayback(conn: org.freedesktop.dbus.connections.impl.DBusConnection) {
        val progressSampled = controller.player.progress.sample(500)
        combine(
            controller.currentItem,
            controller.player.state,
            progressSampled,
        ) { item, state, progress -> Triple(item, state, progress) }
            .distinctUntilChanged()
            .collect { (item, state, progress) ->
                currentProgress = progress
                val newStatus = stateToMpris(state)
                val newMetadata = buildMetadata(item, progress)
                val changed = mutableMapOf<String, Variant<*>>()
                if (newStatus != currentPlaybackStatus) {
                    currentPlaybackStatus = newStatus
                    changed["PlaybackStatus"] = Variant(newStatus)
                }
                if (newMetadata != currentMetadata) {
                    currentMetadata = newMetadata
                    changed["Metadata"] = Variant(newMetadata, "a{sv}")
                }
                changed["Position"] = Variant(progress.position * 1000L, "x")
                runCatching {
                    conn.sendMessage(
                        Properties.PropertiesChanged(
                            OBJECT_PATH,
                            IFACE_PLAYER,
                            changed,
                            emptyList(),
                        )
                    )
                }.onFailure { Log.e(TAG) { "signal failed: $it" } }
            }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun stateToMpris(state: PlaybackPlayer.State) = when (state) {
        PlaybackPlayer.State.Playing -> "Playing"
        PlaybackPlayer.State.Paused  -> "Paused"
        else                         -> "Stopped"
    }

    private fun buildMetadata(
        item: PlayerItem?,
        progress: PlaybackPlayer.Progress,
    ): Map<String, Variant<*>> {
        if (item == null) return emptyMap()
        return buildMap {
            // trackid must be unique per track; use title hash as a stand-in
            val trackId = "/org/musicplayer/track/${item.title.hashCode().and(0x7FFFFFFF)}"
            put("mpris:trackid", Variant(DBusPath(trackId), "o"))
            put("xesam:title",   Variant(item.title))
            item.artist?.let { put("xesam:artist", Variant(listOf(it), "as")) }
            item.album?.let  { put("xesam:album",  Variant(it)) }
            if (progress.duration > 0) {
                put("mpris:length", Variant(progress.duration * 1000L, "x"))
            }
        }
    }

    companion object {
        private const val TAG         = "LinuxMpris"
        private const val BUS_NAME    = "org.mpris.MediaPlayer2.MusicPlayer"
        private const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
        private const val IFACE_ROOT  = "org.mpris.MediaPlayer2"
        private const val IFACE_PLAYER = "org.mpris.MediaPlayer2.Player"
    }
}
