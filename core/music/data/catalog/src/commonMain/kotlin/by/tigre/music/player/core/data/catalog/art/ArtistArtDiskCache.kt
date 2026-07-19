package by.tigre.music.player.core.data.catalog.art

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

internal sealed interface ArtistArtCacheLookup {
    data class Hit(val path: String) : ArtistArtCacheLookup
    data object Miss : ArtistArtCacheLookup
}

internal class ArtistArtDiskCache(
    cacheDirPath: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val missTtlMs: Long = 7L * 24 * 60 * 60 * 1000,
    private val clock: () -> Long = { artistArtEpochMs() },
) {
    private val lock = Any()
    private val cacheDir: Path = cacheDirPath.toPath()
    private val imagesDir: Path = cacheDir / "images"
    private val indexPath: Path = cacheDir / "index.json"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private var index: ArtistArtIndex = loadIndex()

    init {
        fileSystem.createDirectories(imagesDir)
    }

    fun lookup(normalizedName: String): ArtistArtCacheLookup? = artistArtSynchronized(lock) {
        val entry = index.entries[normalizedName] ?: return@artistArtSynchronized null
        when (entry.status) {
            IndexStatus.HIT -> {
                val path = entry.path ?: return@artistArtSynchronized null
                if (!fileSystem.exists(path.toPath())) {
                    index = index.copy(entries = index.entries - normalizedName)
                    persist()
                    null
                } else {
                    ArtistArtCacheLookup.Hit(path)
                }
            }
            IndexStatus.MISS -> {
                if (clock() - entry.updatedAtMs > missTtlMs) {
                    index = index.copy(entries = index.entries - normalizedName)
                    persist()
                    null
                } else {
                    ArtistArtCacheLookup.Miss
                }
            }
        }
    }

    fun putHit(normalizedName: String, bytes: ByteArray): String = artistArtSynchronized(lock) {
        fileSystem.createDirectories(imagesDir)
        val fileName = "${fileKey(normalizedName)}.jpg"
        val path = imagesDir / fileName
        fileSystem.write(path) {
            write(bytes)
        }
        val absolute = path.toString()
        index = index.copy(
            entries = index.entries + (normalizedName to ArtistArtIndexEntry(
                status = IndexStatus.HIT,
                path = absolute,
                updatedAtMs = clock(),
            ))
        )
        persist()
        absolute
    }

    fun putMiss(normalizedName: String): Unit = artistArtSynchronized(lock) {
        index = index.copy(
            entries = index.entries + (normalizedName to ArtistArtIndexEntry(
                status = IndexStatus.MISS,
                path = null,
                updatedAtMs = clock(),
            ))
        )
        persist()
    }

    private fun loadIndex(): ArtistArtIndex {
        if (!fileSystem.exists(indexPath)) return ArtistArtIndex()
        return try {
            val text = fileSystem.source(indexPath).buffer().use { it.readUtf8() }
            json.decodeFromString(ArtistArtIndex.serializer(), text)
        } catch (_: Exception) {
            ArtistArtIndex()
        }
    }

    private fun persist() {
        fileSystem.createDirectories(cacheDir)
        val text = json.encodeToString(ArtistArtIndex.serializer(), index)
        fileSystem.write(indexPath) {
            writeUtf8(text)
        }
    }

    private fun fileKey(normalizedName: String): String =
        normalizedName.hashCode().toUInt().toString(16)

    @Serializable
    private data class ArtistArtIndex(
        val entries: Map<String, ArtistArtIndexEntry> = emptyMap(),
    )

    @Serializable
    private data class ArtistArtIndexEntry(
        val status: IndexStatus,
        val path: String? = null,
        val updatedAtMs: Long,
    )

    @Serializable
    private enum class IndexStatus {
        HIT,
        MISS,
    }
}
