package by.tigre.music.player.core.data.catalog.art

internal expect fun artistArtEpochMs(): Long

internal expect inline fun <R> artistArtSynchronized(lock: Any, block: () -> R): R
