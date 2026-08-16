package by.tigre.media.platform.playback.eq

/**
 * Picks the most specific EQ profile for the current route + optional content.
 * Does not touch storage — operates on an in-memory list.
 */
object EqProfileResolver {

    fun resolve(
        profiles: List<EqProfile>,
        route: AudioRouteId,
        content: EqContentKey,
    ): EqResolveResult {
        val routeKey = route.storageKey()
        val forRoute = profiles.filter { it.route.storageKey() == routeKey }

        when (content) {
            is EqContentKey.Book -> {
                forRoute.firstOrNull { profile ->
                    val key = profile.content
                    key is EqContentKey.Book && key.bookId == content.bookId
                }?.let { return EqResolveResult(it, EqMatchLevel.Book) }
            }
            is EqContentKey.Album -> {
                forRoute.firstOrNull { profile ->
                    val key = profile.content
                    key is EqContentKey.Album && key.albumId == content.albumId
                }?.let { return EqResolveResult(it, EqMatchLevel.Album) }
            }
            is EqContentKey.Artist -> {
                forRoute.firstOrNull { profile ->
                    val key = profile.content
                    key is EqContentKey.Artist && key.artistId == content.artistId
                }?.let { return EqResolveResult(it, EqMatchLevel.Artist) }
            }
            is EqContentKey.Folder -> {
                forRoute.firstOrNull { profile ->
                    val key = profile.content
                    key is EqContentKey.Folder &&
                        key.folderUri == content.folderUri &&
                        key.subPath == content.subPath
                }?.let { return EqResolveResult(it, EqMatchLevel.Folder) }
            }
            EqContentKey.None -> Unit
        }

        forRoute.firstOrNull { it.content is EqContentKey.None }
            ?.let { return EqResolveResult(it, EqMatchLevel.Device) }

        return EqResolveResult(profile = null, matchLevel = EqMatchLevel.None)
    }

    /** Book playback: book → folder → device. */
    fun resolveForBook(
        profiles: List<EqProfile>,
        route: AudioRouteId,
        bookId: Long,
        folderUri: String,
        subPath: String,
    ): EqResolveResult {
        val bookHit = resolve(profiles, route, EqContentKey.Book(bookId))
        if (bookHit.matchLevel == EqMatchLevel.Book) return bookHit

        val folderHit = resolve(profiles, route, EqContentKey.Folder(folderUri, subPath))
        if (folderHit.matchLevel == EqMatchLevel.Folder) return folderHit

        return resolve(profiles, route, EqContentKey.None)
    }

    /** Music playback: album → artist → device. */
    fun resolveForMusic(
        profiles: List<EqProfile>,
        route: AudioRouteId,
        albumId: Long,
        artistId: Long,
    ): EqResolveResult {
        val albumHit = resolve(profiles, route, EqContentKey.Album(albumId))
        if (albumHit.matchLevel == EqMatchLevel.Album) return albumHit

        val artistHit = resolve(profiles, route, EqContentKey.Artist(artistId))
        if (artistHit.matchLevel == EqMatchLevel.Artist) return artistHit

        return resolve(profiles, route, EqContentKey.None)
    }
}
