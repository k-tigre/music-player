package by.tigre.music.player.core.presentation.backgound_player.car

object CarMediaIds {
    const val ROOT = "[ROOT]"

    // Music
    const val TAB_ARTISTS = "tab/artists"
    const val TAB_QUEUE = "tab/queue"
    private const val PREFIX_ARTIST = "artist/"
    private const val PREFIX_ALBUM = "album/"
    private const val PREFIX_SONG = "song/"

    // Audiobook
    const val TAB_BOOKS = "tab/books"
    private const val PREFIX_BOOK = "book/"
    private const val PREFIX_CHAPTER = "chapter/"

    fun artist(id: Long): String = "$PREFIX_ARTIST$id"
    fun album(artistId: Long, albumId: Long): String = "$PREFIX_ALBUM$artistId/$albumId"
    fun song(songId: Long): String = "$PREFIX_SONG$songId"
    fun book(bookId: Long): String = "$PREFIX_BOOK$bookId"
    fun chapter(bookId: Long, chapterId: Long): String = "$PREFIX_CHAPTER$bookId/$chapterId"

    fun parseArtistId(mediaId: String): Long? =
        mediaId.removePrefix(PREFIX_ARTIST).toLongOrNull()

    fun parseAlbumIds(mediaId: String): Pair<Long, Long>? {
        if (!mediaId.startsWith(PREFIX_ALBUM)) return null
        val parts = mediaId.removePrefix(PREFIX_ALBUM).split('/')
        if (parts.size != 2) return null
        val artistId = parts[0].toLongOrNull() ?: return null
        val albumId = parts[1].toLongOrNull() ?: return null
        return artistId to albumId
    }

    fun parseSongId(mediaId: String): Long? =
        mediaId.removePrefix(PREFIX_SONG).toLongOrNull()

    fun parseBookId(mediaId: String): Long? =
        mediaId.removePrefix(PREFIX_BOOK).toLongOrNull()

    fun parseChapterIds(mediaId: String): Pair<Long, Long>? {
        if (!mediaId.startsWith(PREFIX_CHAPTER)) return null
        val parts = mediaId.removePrefix(PREFIX_CHAPTER).split('/')
        if (parts.size != 2) return null
        val bookId = parts[0].toLongOrNull() ?: return null
        val chapterId = parts[1].toLongOrNull() ?: return null
        return bookId to chapterId
    }
}
