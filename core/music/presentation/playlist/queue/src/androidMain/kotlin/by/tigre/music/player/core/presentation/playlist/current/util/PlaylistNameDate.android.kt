package by.tigre.music.player.core.presentation.playlist.current.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal actual fun currentDateForPlaylistName(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
