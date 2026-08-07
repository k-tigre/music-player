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
                forRoute.firstOrNull {
                    it.content is EqContentKey.Book &&
                        (it.content as EqContentKey.Book).bookId == content.bookId
                }?.let { return EqResolveResult(it, EqMatchLevel.Book) }
            }
            is EqContentKey.Folder -> {
                // book branch not applicable
            }
            EqContentKey.None -> Unit
        }

        val folderKey: EqContentKey.Folder? = when (content) {
            is EqContentKey.Book -> null // caller may also pass folder via dual resolve
            is EqContentKey.Folder -> content
            EqContentKey.None -> null
        }

        if (folderKey != null) {
            forRoute.firstOrNull {
                it.content is EqContentKey.Folder &&
                    (it.content as EqContentKey.Folder).folderUri == folderKey.folderUri &&
                    (it.content as EqContentKey.Folder).subPath == folderKey.subPath
            }?.let { return EqResolveResult(it, EqMatchLevel.Folder) }
        }

        forRoute.firstOrNull { it.content is EqContentKey.None }
            ?.let { return EqResolveResult(it, EqMatchLevel.Device) }

        return EqResolveResult(profile = null, matchLevel = EqMatchLevel.None)
    }

    /**
     * Book playback resolve: try book, then folder, then device.
     */
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
}
