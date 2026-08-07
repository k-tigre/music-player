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

        if (content is EqContentKey.Book) {
            forRoute.firstOrNull { profile ->
                val key = profile.content
                key is EqContentKey.Book && key.bookId == content.bookId
            }?.let { return EqResolveResult(it, EqMatchLevel.Book) }
        }

        val folderKey = content as? EqContentKey.Folder
        if (folderKey != null) {
            forRoute.firstOrNull { profile ->
                val key = profile.content
                key is EqContentKey.Folder &&
                    key.folderUri == folderKey.folderUri &&
                    key.subPath == folderKey.subPath
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
