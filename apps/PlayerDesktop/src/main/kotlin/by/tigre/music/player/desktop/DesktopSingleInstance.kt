package by.tigre.music.player.desktop

import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Allows only one GUI process: a second launch exits immediately after [acquireOrExit].
 * Uses a file lock in the user home directory — no network, no second-instance signaling.
 */
internal object DesktopSingleInstance {
    private val lockFile: Path = Paths.get(
        System.getProperty("user.home"),
        ".music-player-desktop",
        "instance.lock",
    )

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /**
     * If another instance holds the lock, the current process should exit without starting UI.
     */
    fun acquireOrExit() {
        try {
            Files.createDirectories(lockFile.parent)
        } catch (_: IOException) {
            // Fall through; RandomAccessFile will fail if path is unusable
        }
        val ch = RandomAccessFile(lockFile.toFile(), "rw").channel
        val l = ch.tryLock()
        if (l == null) {
            try {
                ch.close()
            } catch (_: IOException) {
            }
            kotlin.system.exitProcess(0)
        }
        channel = ch
        lock = l
    }

    fun shutdown() {
        try {
            lock?.release()
        } catch (_: IOException) {
        }
        try {
            channel?.close()
        } catch (_: IOException) {
        }
        lock = null
        channel = null
    }
}
