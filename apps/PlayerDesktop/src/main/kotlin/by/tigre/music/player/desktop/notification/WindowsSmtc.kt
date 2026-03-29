package by.tigre.music.player.desktop.notification

import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.core.presentation.catalog.component.BasePlaybackController
import by.tigre.music.player.core.presentation.catalog.component.PlayerItem
import by.tigre.music.player.logger.Log
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinDef.HMENU
import com.sun.jna.platform.win32.WinDef.HINSTANCE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * Windows System Media Transport Controls (SMTC) integration via JNA + WinRT COM.
 *
 * Registers the app with the Windows media widget (taskbar flyout, lock screen,
 * keyboard media keys, Bluetooth headset buttons) so users can control playback
 * from outside the app window.
 *
 * Architecture:
 *  1. CoInitializeEx / RoInitialize — required before any WinRT call
 *  2. A message-only HWND is created — SMTC requires a window handle
 *  3. ISystemMediaTransportControlsInterop.GetForWindow → SMTC COM pointer
 *  4. Buttons are enabled and a ButtonPressed event handler is registered
 *  5. A coroutine observes playback state and pushes updates to SMTC
 *
 * COM vtable dispatch is done via JNA [Function.getFunction] to avoid generating
 * a full COM type library.  Indices are based on the Windows SDK 10.0.22621 headers.
 */
class WindowsSmtc(private val controller: BasePlaybackController) {

    // ── Native interfaces ──────────────────────────────────────────────────────

    private interface ComBase : Library {
        /** RO_INIT_MULTITHREADED = 1 */
        fun RoInitialize(initType: Int): Int
        fun RoUninitialize()
        fun RoGetActivationFactory(activatableClassId: Pointer, riid: ByteArray, factory: Array<Pointer?>): Int
        fun WindowsCreateString(sourceString: WString, length: Int, string: Array<Pointer?>): Int
        fun WindowsDeleteString(string: Pointer?): Int

        companion object {
            val INSTANCE: ComBase = Native.load("combase", ComBase::class.java)
        }
    }

    // ── JNA Callbacks for COM interface implementation ─────────────────────────

    /** IUnknown::QueryInterface */
    private interface QiCallback : Callback {
        fun invoke(self: Pointer, riid: Pointer, ppv: Pointer): Int
    }
    /** IUnknown::AddRef / Release */
    private interface RefCallback : Callback {
        fun invoke(self: Pointer): Int
    }
    /** ITypedEventHandler<SMTC*, ButtonPressedArgs*>::Invoke */
    private interface ButtonInvokeCallback : Callback {
        fun invoke(self: Pointer, sender: Pointer, args: Pointer): Int
    }

    // ── COM vtable helper ──────────────────────────────────────────────────────

    /** Calls vtable method [index] on COM object [ptr], passing [ptr] as `this`. */
    private fun vtable(ptr: Pointer, index: Int, vararg extra: Any?): Int {
        val vt = ptr.getPointer(0)
        val fn = Function.getFunction(vt.getPointer(index.toLong() * Native.POINTER_SIZE))
        return fn.invoke(Int::class.java, arrayOf<Any?>(ptr, *extra)) as Int
    }

    // ── HSTRING helpers ────────────────────────────────────────────────────────

    private val comBase = ComBase.INSTANCE

    private fun hstring(text: String): Pointer? {
        val out = arrayOfNulls<Pointer>(1)
        val hr = comBase.WindowsCreateString(WString(text), text.length, out)
        return if (hr == 0) out[0] else null
    }

    private fun freeHstring(h: Pointer?) {
        if (h != null) comBase.WindowsDeleteString(h)
    }

    /** Reads an HSTRING's content via WindowsGetStringRawBuffer (no extra dep needed). */
    private fun readHstring(h: Pointer): String? = runCatching {
        // WindowsGetStringRawBuffer is also in combase.dll
        val fn = Function.getFunction("combase", "WindowsGetStringRawBuffer")
        val lenOut = Memory(4)
        val wptr = fn.invoke(Pointer::class.java, arrayOf<Any?>(h, lenOut)) as? Pointer
            ?: return@runCatching null
        val len = lenOut.getInt(0)
        wptr.getWideString(0).take(len)
    }.getOrNull()

    // ── GUID helper ───────────────────────────────────────────────────────────
    // GUIDs are packed as 4+2+2+8 bytes in little-endian order

    private fun guid(data1: Int, data2: Short, data3: Short, vararg data4: Byte): ByteArray {
        val buf = Memory(16)
        buf.setInt(0, data1)
        buf.setShort(4, data2)
        buf.setShort(6, data3)
        for (i in data4.indices) buf.setByte(8L + i, data4[i])
        return buf.getByteArray(0, 16)
    }

    // ISystemMediaTransportControlsInterop  {ddb0472d-c911-4a1f-86d9-dc3d71a95f5a}
    private val IID_SMTC_INTEROP = guid(
        0xddb0472d.toInt(), 0xc911.toShort(), 0x4a1f.toShort(),
        0x86.toByte(), 0xd9.toByte(), 0xdc.toByte(), 0x3d.toByte(),
        0x71.toByte(), 0xa9.toByte(), 0x5f.toByte(), 0x5a.toByte()
    )
    // ISystemMediaTransportControls  {99fa3ff4-1742-42a6-902e-087d41f965ec}
    private val IID_SMTC = guid(
        0x99fa3ff4.toInt(), 0x1742.toShort(), 0x42a6.toShort(),
        0x90.toByte(), 0x2e.toByte(), 0x08.toByte(), 0x7d.toByte(),
        0x41.toByte(), 0xf9.toByte(), 0x65.toByte(), 0xec.toByte()
    )

    // ── State ──────────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var hwnd: WinDef.HWND? = null
    private var smtc: Pointer? = null
    private var displayUpdater: Pointer? = null
    private val callbacks = mutableListOf<Callback>() // keep strong refs

    // ── Public API ─────────────────────────────────────────────────────────────

    fun start() {
        runCatching {
            comBase.RoInitialize(1 /* RO_INIT_MULTITHREADED */)
            hwnd = createMessageWindow()
            smtc = getSmtcForWindow(hwnd!!)
            enableButtons(smtc!!)
            displayUpdater = getDisplayUpdater(smtc!!)
            registerButtonHandler(smtc!!)
            scope.launch { observePlayback() }
            Log.i(TAG) { "started" }
        }.onFailure { Log.e(TAG) { "start failed: $it" } }
    }

    fun stop() {
        scope.cancel()
        runCatching { displayUpdater?.let { vtable(it, 2 /* Release */) } }
        runCatching { smtc?.let { vtable(it, 2 /* Release */) } }
        runCatching { hwnd?.let { com.sun.jna.platform.win32.User32.INSTANCE.DestroyWindow(it) } }
        runCatching { comBase.RoUninitialize() }
        callbacks.clear()
        Log.i(TAG) { "stopped" }
    }

    // ── Initialisation helpers ─────────────────────────────────────────────────

    private fun createMessageWindow(): WinDef.HWND {
        val user32 = com.sun.jna.platform.win32.User32.INSTANCE
        // HWND_MESSAGE = -3 (message-only window, no screen presence)
        val hwndMessage = WinDef.HWND(Pointer(-3L))
        val hwnd = user32.CreateWindowEx(
            0,               // dwExStyle
            "STATIC",        // predefined class — no registration needed
            "MusicPlayer",   // window name
            0,               // dwStyle
            0, 0, 0, 0,
            hwndMessage,     // parent = HWND_MESSAGE
            null as HMENU?,
            null as HINSTANCE?,
            null             // lpParam
        ) ?: error("CreateWindowEx failed")
        return hwnd
    }

    private fun getSmtcForWindow(hwnd: WinDef.HWND): Pointer {
        // Get the activation factory for Windows.Media.SystemMediaTransportControls
        val classNameHs = hstring("Windows.Media.SystemMediaTransportControls")
            ?: error("Failed to create class name HSTRING")
        try {
            val factoryOut = arrayOfNulls<Pointer>(1)
            val hr = comBase.RoGetActivationFactory(classNameHs, IID_SMTC_INTEROP, factoryOut)
            check(hr == 0) { "RoGetActivationFactory failed: 0x${hr.toUInt().toString(16)}" }
            val interop = factoryOut[0] ?: error("Factory is null")

            // ISystemMediaTransportControlsInterop::GetForWindow  (vtable index 6)
            // HRESULT GetForWindow(HWND, REFIID, void**)
            val smtcOut = arrayOfNulls<Pointer>(1)
            val iidMem = Memory(16)
            iidMem.write(0, IID_SMTC, 0, 16)
            val hr2 = vtable(interop, 6, hwnd.pointer, iidMem, smtcOut)
            vtable(interop, 2 /* Release */)
            check(hr2 == 0) { "GetForWindow failed: 0x${hr2.toUInt().toString(16)}" }
            return smtcOut[0] ?: error("SMTC pointer is null")
        } finally {
            freeHstring(classNameHs)
        }
    }

    private fun enableButtons(smtc: Pointer) {
        // put_IsEnabled(true)=20, put_IsPlayEnabled(true)=21,
        // put_IsPauseEnabled(true)=22, put_IsNextEnabled(true)=23, put_IsPreviousEnabled(true)=24
        for (index in 20..24) vtable(smtc, index, 1)
    }

    private fun getDisplayUpdater(smtc: Pointer): Pointer {
        // get_DisplayUpdater  (vtable index 8)
        val out = arrayOfNulls<Pointer>(1)
        val hr = vtable(smtc, 8, out)
        check(hr == 0) { "get_DisplayUpdater failed" }
        // Set type to Music (1 = MediaPlaybackType.Music)
        // put_Type  (ISystemMediaTransportControlsDisplayUpdater vtable index 7)
        val updater = out[0] ?: error("DisplayUpdater is null")
        vtable(updater, 7, 1)
        return updater
    }

    private fun registerButtonHandler(smtc: Pointer) {
        // Build COM object implementing ITypedEventHandler (IUnknown-only base):
        //   vtable[0] = QueryInterface
        //   vtable[1] = AddRef
        //   vtable[2] = Release
        //   vtable[3] = Invoke(self, sender, args) → read args.get_Button → dispatch

        val qi = object : QiCallback {
            override fun invoke(self: Pointer, riid: Pointer, ppv: Pointer): Int {
                ppv.setPointer(0, self)
                return 0
            }
        }.also { callbacks.add(it) }

        val addRef = object : RefCallback {
            override fun invoke(self: Pointer) = 1
        }.also { callbacks.add(it) }

        val release = object : RefCallback {
            override fun invoke(self: Pointer) = 1
        }.also { callbacks.add(it) }

        val invoke = object : ButtonInvokeCallback {
            override fun invoke(self: Pointer, sender: Pointer, args: Pointer): Int {
                runCatching {
                    // ISystemMediaTransportControlsButtonPressedEventArgs::get_Button (vtable index 6)
                    val buttonOut = Memory(4)
                    vtable(args, 6, buttonOut)
                    when (buttonOut.getInt(0)) {
                        BUTTON_PLAY     -> controller.resume()
                        BUTTON_PAUSE    -> controller.pause()
                        BUTTON_NEXT     -> controller.playNext()
                        BUTTON_PREVIOUS -> controller.playPrev()
                    }
                }
                return 0 // S_OK
            }
        }.also { callbacks.add(it) }

        val vtableMem = Memory(4L * Native.POINTER_SIZE)
        vtableMem.setPointer(0L,                    CallbackReference.getFunctionPointer(qi))
        vtableMem.setPointer(1L * Native.POINTER_SIZE,     CallbackReference.getFunctionPointer(addRef))
        vtableMem.setPointer(2L * Native.POINTER_SIZE,     CallbackReference.getFunctionPointer(release))
        vtableMem.setPointer(3L * Native.POINTER_SIZE,     CallbackReference.getFunctionPointer(invoke))

        val handlerMem = Memory(Native.POINTER_SIZE.toLong())
        handlerMem.setPointer(0, vtableMem)

        // add_ButtonPressed  (vtable index 27)
        // HRESULT add_ButtonPressed(handler, EventRegistrationToken*)
        val tokenOut = Memory(8)
        vtable(smtc, 27, handlerMem, tokenOut)
    }

    // ── Playback observation ───────────────────────────────────────────────────

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
                runCatching { updateSmtc(item, state, progress) }
                    .onFailure { Log.e(TAG) { "update failed: $it" } }
            }
    }

    private fun updateSmtc(
        item: PlayerItem?,
        state: PlaybackPlayer.State,
        progress: PlaybackPlayer.Progress,
    ) {
        val smtc = smtc ?: return
        val updater = displayUpdater ?: return

        // put_PlaybackStatus  (vtable index 7)
        // SystemMediaTransportControlsPlaybackStatus: Closed=0 Changing=1 Stopped=2 Playing=3 Paused=4
        val playbackStatus = when (state) {
            PlaybackPlayer.State.Playing -> 3
            PlaybackPlayer.State.Paused  -> 4
            PlaybackPlayer.State.Idle    -> 2
            PlaybackPlayer.State.Ended   -> 2
        }
        vtable(smtc, 7, playbackStatus)

        if (item != null) {
            // get_MusicProperties  (DisplayUpdater vtable index 12)
            val musicOut = arrayOfNulls<Pointer>(1)
            if (vtable(updater, 12, musicOut) != 0) return
            val music = musicOut[0] ?: return

            // put_Title  (IMusicDisplayProperties vtable index 7)
            hstring(item.title)?.let { h ->
                vtable(music, 7, h)
                freeHstring(h)
            }
            // put_Artist  (IMusicDisplayProperties vtable index 11)
            item.artist?.let { artist ->
                hstring(artist)?.let { h ->
                    vtable(music, 11, h)
                    freeHstring(h)
                }
            }
            // put_AlbumArtist  (IMusicDisplayProperties vtable index 9)
            (item.album ?: item.artist)?.let { album ->
                hstring(album)?.let { h ->
                    vtable(music, 9, h)
                    freeHstring(h)
                }
            }
            vtable(music, 2 /* Release */)

            // Update  (DisplayUpdater vtable index 15)
            vtable(updater, 15)
        }
    }

    companion object {
        private const val TAG = "WindowsSmtc"

        // SystemMediaTransportControlsButton enum values
        private const val BUTTON_PLAY     = 0
        private const val BUTTON_PAUSE    = 1
        private const val BUTTON_NEXT     = 6
        private const val BUTTON_PREVIOUS = 7
    }
}
