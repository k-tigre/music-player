package by.tigre.music.player.core.data.storage.playlist.impl

import app.cash.sqldelight.ColumnAdapter
import by.tigre.music.player.core.entiry.playlist.PlaylistKind

object PlaylistKindAdapter : ColumnAdapter<PlaylistKind, Long> {
    override fun decode(databaseValue: Long): PlaylistKind = when (databaseValue) {
        1L -> PlaylistKind.Imported
        2L -> PlaylistKind.Smart
        else -> PlaylistKind.User
    }

    override fun encode(value: PlaylistKind): Long = when (value) {
        PlaylistKind.User -> 0L
        PlaylistKind.Imported -> 1L
        PlaylistKind.Smart -> 2L
    }
}
