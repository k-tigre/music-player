package by.tigre.music.player.desktop.notification

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.logger.Log
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * macOS Now Playing integration via JNA + Objective-C runtime.
 *
 * Sets [MPNowPlayingInfoCenter] so the app appears in Control Center "Now Playing" widget,
 * and registers [MPRemoteCommandCenter] handlers so the user can control playback
 * from the system (media keys, Control Center, AirPods, etc.).
 */
class MacOsNowPlaying(private val controller: BasePlaybackController) {

    // ── JNA interfaces ────────────────────────────────────────────────────────
    //
    // Each method here maps to the SAME native symbol "objc_msgSend" via FunctionMapper.
    // Typed parameters tell JNA exactly how to marshal each argument, avoiding the
    // pointer-conversion bugs that happen with `vararg Any?` on arm64.

    private interface ObjCMsg : Library {
        fun msg0(receiver: Pointer?, sel: Pointer): Pointer?
        fun msg1p(receiver: Pointer?, sel: Pointer, arg: Pointer?): Pointer?
        fun msg2p(receiver: Pointer?, sel: Pointer, a: Pointer?, b: Pointer?): Pointer?
        fun msg1i(receiver: Pointer?, sel: Pointer, arg: Int): Pointer?
        fun msgStr(receiver: Pointer?, sel: Pointer, bytes: ByteArray): Pointer?  // const char *
        fun msgDbl(receiver: Pointer?, sel: Pointer, value: Double): Pointer?     // double
        fun msgTarget(receiver: Pointer?, sel: Pointer, target: Pointer?, action: Pointer): Pointer?

        companion object {
            val INSTANCE: ObjCMsg = Native.load(
                "objc", ObjCMsg::class.java,
                mapOf(Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, _ -> "objc_msgSend" })
            )
        }
    }

    private interface ObjCRuntime : Library {
        fun objc_getClass(name: String): Pointer?
        fun sel_registerName(name: String): Pointer
        fun objc_allocateClassPair(superclass: Pointer?, name: String, extraBytes: Long): Pointer?
        fun objc_registerClassPair(cls: Pointer)
        fun class_addMethod(cls: Pointer, name: Pointer, imp: Pointer, types: String): Boolean

        companion object {
            val INSTANCE: ObjCRuntime = Native.load("objc", ObjCRuntime::class.java)
        }
    }

    // Double-returning msgSend: arm64 uses objc_msgSend, x86_64 uses objc_msgSend_fpret
    private interface ObjCFPRet : Library {
        fun getDouble(receiver: Pointer?, sel: Pointer): Double

        companion object {
            private val fn = if (System.getProperty("os.arch") == "aarch64") "objc_msgSend" else "objc_msgSend_fpret"
            val INSTANCE: ObjCFPRet = Native.load(
                "objc", ObjCFPRet::class.java,
                mapOf(Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, m ->
                    if (m.name == "getDouble") fn else m.name
                })
            )
        }
    }

    // ObjC method IMP: (NSInteger)handle:(MPRemoteCommandEvent *)event  →  type "q@:@"
    private interface CommandImp : Callback {
        fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private val rt = ObjCRuntime.INSTANCE
    private val m = ObjCMsg.INSTANCE
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val callbacks = mutableListOf<Callback>() // strong refs → prevent GC

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun cls(name: String) = rt.objc_getClass(name)
    private fun sel(name: String) = rt.sel_registerName(name)

    /** Creates an NSString; ByteArray is properly marshalled by JNA as const char*. */
    private fun nsString(str: String): Pointer? {
        val bytes = str.toByteArray(Charsets.UTF_8) + byteArrayOf(0)
        return m.msgStr(cls("NSString"), sel("stringWithUTF8String:"), bytes)
    }

    private fun nsDouble(d: Double): Pointer? =
        m.msgDbl(cls("NSNumber"), sel("numberWithDouble:"), d)

    // ── Public API ────────────────────────────────────────────────────────────

    fun start() {
        runCatching {
            loadMediaPlayerFramework()
            registerCommandHandlers()
            scope.launch { observePlayback() }
            Log.i(TAG) { "started" }
        }.onFailure {
            Log.e(TAG) { "start failed: $it" }
        }
    }

    fun stop() {
        scope.cancel()
        callbacks.clear()
        runCatching { clearNowPlaying() }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun loadMediaPlayerFramework() {
        val path = nsString("/System/Library/Frameworks/MediaPlayer.framework") ?: return
        val bundle = m.msg1p(cls("NSBundle"), sel("bundleWithPath:"), path) ?: return
        m.msg0(bundle, sel("load"))
    }

    private fun registerCommandHandlers() {
        val nsobject = cls("NSObject") ?: error("NSObject not found")
        val clsName = "JVMNowPlayingTarget_${System.nanoTime()}"
        val targetCls = rt.objc_allocateClassPair(nsobject, clsName, 0)
            ?: error("objc_allocateClassPair failed")

        fun makeImp(action: () -> Unit): Pointer {
            val cb = object : CommandImp {
                override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                    runCatching { action() }
                    return 0L // MPRemoteCommandHandlerStatusSuccess
                }
            }.also { callbacks.add(it) }
            return CallbackReference.getFunctionPointer(cb)
        }

        val seekCb = object : CommandImp {
            override fun invoke(self: Pointer, cmd: Pointer, event: Pointer): Long {
                runCatching {
                    val seconds = ObjCFPRet.INSTANCE.getDouble(event, sel("positionTime"))
                    scope.launch { controller.player.seekTo((seconds * 1000).toLong()) }
                }
                return 0L
            }
        }.also { callbacks.add(it) }

        val types = "q@:@"
        rt.class_addMethod(targetCls, sel("handlePlay:"),  makeImp { controller.resume() },   types)
        rt.class_addMethod(targetCls, sel("handlePause:"), makeImp { controller.pause() },    types)
        rt.class_addMethod(targetCls, sel("handleNext:"),  makeImp { controller.playNext() }, types)
        rt.class_addMethod(targetCls, sel("handlePrev:"),  makeImp { controller.playPrev() }, types)
        rt.class_addMethod(targetCls, sel("handleSeek:"),  CallbackReference.getFunctionPointer(seekCb), types)
        rt.objc_registerClassPair(targetCls)

        val target = m.msg0(m.msg0(targetCls, sel("alloc")), sel("init"))
            ?: error("target alloc/init returned nil")
        val commandCenter = m.msg0(cls("MPRemoteCommandCenter"), sel("sharedCommandCenter"))
            ?: error("MPRemoteCommandCenter not found")

        listOf(
            "playCommand"          to "handlePlay:",
            "pauseCommand"         to "handlePause:",
            "nextTrackCommand"     to "handleNext:",
            "previousTrackCommand" to "handlePrev:",
        ).forEach { (cmd, handler) ->
            m.msg0(commandCenter, sel(cmd))?.let { command ->
                m.msg1i(command, sel("setEnabled:"), 1)
                m.msgTarget(command, sel("addTarget:action:"), target, sel(handler))
            }
        }

        m.msg0(commandCenter, sel("changePlaybackPositionCommand"))?.let { seekCmd ->
            m.msg1i(seekCmd, sel("setEnabled:"), 1)
            m.msgTarget(seekCmd, sel("addTarget:action:"), target, sel("handleSeek:"))
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private suspend fun observePlayback() {
        val progressSampled = controller.player.progress.sample(500)
        combine(
            controller.currentItem,
            controller.player.state,
            progressSampled,
        ) { item, state, progress -> Triple(item, state, progress) }
            .distinctUntilChanged()
            .collect { (item, state, progress) ->
                runCatching {
                    if (item != null) updateNowPlaying(item, state, progress)
                    else clearNowPlaying()
                }.onFailure { Log.e(TAG) { "update failed: $it" } }
            }
    }

    private fun updateNowPlaying(
        item: PlayerItem,
        state: PlaybackPlayer.State,
        progress: PlaybackPlayer.Progress,
    ) {
        val dict = m.msg0(cls("NSMutableDictionary"), sel("dictionary")) ?: return

        fun set(key: String, value: Pointer?) {
            if (value != null) m.msg2p(dict, sel("setObject:forKey:"), value, nsString(key))
        }

        set("title", nsString(item.title))
        item.artist?.let { set("artist", nsString(it)) }
        item.album?.let { set("albumTitle", nsString(it)) }

        if (progress.duration > 0) {
            set("playbackDuration", nsDouble(progress.duration / 1000.0))
            set("MPNowPlayingInfoPropertyElapsedPlaybackTime", nsDouble(progress.position / 1000.0))
        }
        set("MPNowPlayingInfoPropertyPlaybackRate",
            nsDouble(if (state == PlaybackPlayer.State.Playing) 1.0 else 0.0))

        m.msg0(cls("MPNowPlayingInfoCenter"), sel("defaultCenter"))
            ?.let { center -> m.msg1p(center, sel("setNowPlayingInfo:"), dict) }
    }

    private fun clearNowPlaying() {
        m.msg0(cls("MPNowPlayingInfoCenter"), sel("defaultCenter"))
            ?.let { center -> m.msg1p(center, sel("setNowPlayingInfo:"), null) }
    }

    companion object {
        private const val TAG = "MacOsNowPlaying"
    }
}
