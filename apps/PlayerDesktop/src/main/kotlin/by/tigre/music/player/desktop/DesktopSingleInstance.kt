package by.tigre.music.player.desktop

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Ensures only one GUI process: a second launch connects here and exits; the first instance
 * raises its windows. Uses loopback TCP — no data leaves the machine.
 */
internal object DesktopSingleInstance {
    private const val PORT = 49263
    private const val LINE = "FOREGROUND"
    private const val CONNECT_TIMEOUT_MS = 500
    private const val RACE_RETRY_MS = 250L

    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null

    /**
     * @return `true` if another instance was signaled and this JVM should exit without starting UI.
     */
    fun handOffToRunningInstanceIfAny(): Boolean {
        if (notifyRunningInstance()) return true
        return try {
            startServer()
            false
        } catch (_: BindException) {
            Thread.sleep(RACE_RETRY_MS)
            if (notifyRunningInstance()) true
            else {
                startServer()
                false
            }
        }
    }

    private fun notifyRunningInstance(): Boolean =
        try {
            Socket().use { socket ->
                socket.soTimeout = CONNECT_TIMEOUT_MS
                socket.connect(
                    InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT),
                    CONNECT_TIMEOUT_MS,
                )
                socket.getOutputStream().bufferedWriter(StandardCharsets.UTF_8).use { out ->
                    out.write(LINE)
                    out.newLine()
                    out.flush()
                }
                // Drain so the server can accept the next connection cleanly
                socket.shutdownOutput()
                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).readLine()
            }
            true
        } catch (_: Exception) {
            false
        }

    private fun startServer() {
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT))
        server = ss
        acceptThread = thread(name = "music-player-single-instance", isDaemon = true) {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    ss.accept().use { client ->
                        BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)).readLine()
                        client.getOutputStream().bufferedWriter(StandardCharsets.UTF_8).use { out ->
                            out.write("OK")
                            out.newLine()
                            out.flush()
                        }
                        SwingUtilities.invokeLater {
                            AppWindowGroup.onSecondInstanceActivated()
                        }
                    }
                } catch (_: SocketException) {
                    break
                } catch (_: IOException) {
                    break
                }
            }
        }
    }

    fun shutdown() {
        acceptThread?.interrupt()
        acceptThread = null
        try {
            server?.close()
        } catch (_: IOException) {
        }
        server = null
    }
}
