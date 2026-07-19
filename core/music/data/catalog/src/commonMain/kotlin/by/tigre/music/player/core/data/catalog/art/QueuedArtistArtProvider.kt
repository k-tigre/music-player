package by.tigre.music.player.core.data.catalog.art

import by.tigre.music.player.core.data.catalog.ArtistArt
import by.tigre.music.player.core.data.catalog.ArtistArtModel
import by.tigre.music.player.core.data.catalog.ArtistArtProvider
import by.tigre.music.player.core.data.catalog.ArtistArtRemote
import by.tigre.music.player.core.entiry.catalog.Artist
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

internal class QueuedArtistArtProvider(
    private val cache: ArtistArtDiskCache,
    private val remote: ArtistArtRemote,
    private val httpClient: HttpClient,
    private val minIntervalMs: Long = 400L,
    private val maxAttempts: Int = 3,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ArtistArtProvider {

    private val statesLock = Any()
    private val states = mutableMapOf<String, MutableStateFlow<ArtistArt>>()
    private val enqueueMutex = Mutex()
    private val pendingKeys = mutableSetOf<String>()
    private val queue = Channel<String>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (key in queue) {
                processKey(key)
                delay(minIntervalMs)
            }
        }
    }

    override fun observe(artistId: Artist.Id, name: String): Flow<ArtistArt> {
        val key = ArtistNameNormalizer.normalize(name)
        return stateFlow(key).asStateFlow()
    }

    override fun request(artistId: Artist.Id, name: String) {
        val key = ArtistNameNormalizer.normalize(name)
        if (key.isEmpty()) return
        scope.launch {
            val state = stateFlow(key)
            when (state.value) {
                is ArtistArt.Ready, ArtistArt.Missing, ArtistArt.Loading -> return@launch
                ArtistArt.Idle -> Unit
            }
            when (val cached = cache.lookup(key)) {
                is ArtistArtCacheLookup.Hit -> {
                    state.value = ArtistArt.Ready(ArtistArtModel.File(cached.path))
                    return@launch
                }
                ArtistArtCacheLookup.Miss -> {
                    state.value = ArtistArt.Missing
                    return@launch
                }
                null -> Unit
            }
            state.value = ArtistArt.Loading
            enqueueMutex.withLock {
                if (key in pendingKeys) return@launch
                pendingKeys.add(key)
                queue.send(key)
            }
        }
    }

    private fun stateFlow(key: String): MutableStateFlow<ArtistArt> {
        artistArtSynchronized(statesLock) {
            states[key]?.let { return it }
        }
        val initial = when (val cached = cache.lookup(key)) {
            is ArtistArtCacheLookup.Hit -> ArtistArt.Ready(ArtistArtModel.File(cached.path))
            ArtistArtCacheLookup.Miss -> ArtistArt.Missing
            null -> ArtistArt.Idle
        }
        return artistArtSynchronized(statesLock) {
            states.getOrPut(key) { MutableStateFlow(initial) }
        }
    }

    private suspend fun processKey(key: String) {
        val state = stateFlow(key)
        try {
            when (val cached = cache.lookup(key)) {
                is ArtistArtCacheLookup.Hit -> {
                    state.value = ArtistArt.Ready(ArtistArtModel.File(cached.path))
                    return
                }
                ArtistArtCacheLookup.Miss -> {
                    state.value = ArtistArt.Missing
                    return
                }
                null -> Unit
            }

            var attempt = 0
            var backoffMs = 1_000L
            while (attempt < maxAttempts) {
                attempt++
                try {
                    val url = remote.findImageUrl(key)
                    if (url == null) {
                        cache.putMiss(key)
                        state.value = ArtistArt.Missing
                        return
                    }
                    val bytes = downloadImage(url)
                    val path = cache.putHit(key, bytes)
                    state.value = ArtistArt.Ready(ArtistArtModel.File(path))
                    return
                } catch (_: ArtistArtRateLimitedException) {
                    if (attempt >= maxAttempts) break
                    delay(backoffWithJitter(backoffMs))
                    backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                } catch (e: ArtistArtHttpException) {
                    if (e.code in 500..599 || e.code == 429) {
                        if (attempt >= maxAttempts) break
                        delay(backoffWithJitter(backoffMs))
                        backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                    } else {
                        cache.putMiss(key)
                        state.value = ArtistArt.Missing
                        return
                    }
                } catch (_: Exception) {
                    if (attempt >= maxAttempts) break
                    delay(backoffWithJitter(backoffMs))
                    backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                }
            }
            cache.putMiss(key)
            state.value = ArtistArt.Missing
        } finally {
            enqueueMutex.withLock { pendingKeys.remove(key) }
        }
    }

    private suspend fun downloadImage(url: String): ByteArray {
        val response = httpClient.get(url)
        when (response.status) {
            HttpStatusCode.TooManyRequests -> throw ArtistArtRateLimitedException()
            else -> {
                if (!response.status.isSuccess()) {
                    throw ArtistArtHttpException(response.status.value)
                }
            }
        }
        return response.readRawBytes()
    }

    private fun backoffWithJitter(baseMs: Long): Long {
        val jitter = Random.nextLong(0, (baseMs / 2).coerceAtLeast(1))
        return baseMs + jitter
    }
}
