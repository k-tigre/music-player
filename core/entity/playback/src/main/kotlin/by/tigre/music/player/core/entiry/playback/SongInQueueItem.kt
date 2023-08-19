package by.tigre.music.player.core.entiry.playback

import by.tigre.music.player.core.entiry.catalog.Song

data class SongInQueueItem(val song: Song, val isPlaying: Boolean)