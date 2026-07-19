package by.tigre.music.player.core.data.catalog.art

internal actual fun artistArtEpochMs(): Long = System.currentTimeMillis()

internal actual inline fun <R> artistArtSynchronized(lock: Any, block: () -> R): R =
    synchronized(lock, block)
