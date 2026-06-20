package by.tigre.music.player.core.data.playback.impl

import by.tigre.media.platform.playback.MediaItemWrapper
import by.tigre.media.platform.playback.PlaybackPlayer
import by.tigre.media.platform.tools.coroutines.CoreScope
import by.tigre.music.player.core.data.catalog.CatalogSource
import by.tigre.music.player.core.data.storage.playback_queue.PlaybackQueueStorage
import by.tigre.music.player.core.entiry.catalog.Album
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.music.player.core.entiry.catalog.CatalogSearchResult
import by.tigre.music.player.core.entiry.catalog.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackControllerImplTest {

    @Test
    fun resumeWhenQueueEndedRestartsQueueViaPlayNext() = runBlocking {
        val storage = FakePlaybackQueueStorage(
            queue = listOf(
                queueItem(id = 1, songId = 1, state = PlaybackQueueStorage.QueueItem.State.Finish),
                queueItem(id = 2, songId = 2, state = PlaybackQueueStorage.QueueItem.State.Finish),
            )
        )
        val controller = PlaybackControllerImpl(
            storage = storage,
            catalog = FakeCatalogSource(),
            player = FakePlaybackPlayer(state = PlaybackPlayer.State.Ended),
            scope = TestCoreScope(),
        )

        controller.resume()
        waitForCondition { storage.playNextCalls == 1 }

        assertEquals(1, storage.playNextCalls)
    }

    private suspend fun waitForCondition(timeoutMs: Long = 500, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            delay(10)
        }
        assertEquals(true, condition())
    }

    private fun queueItem(
        id: Long,
        songId: Long,
        state: PlaybackQueueStorage.QueueItem.State,
    ) = PlaybackQueueStorage.QueueItem(
        id = id,
        songsId = Song.Id(songId),
        state = state,
    )

    private class TestCoreScope : CoreScope, CoroutineScope by CoroutineScope(Job() + Dispatchers.Unconfined)

    private class FakePlaybackQueueStorage(
        queue: List<PlaybackQueueStorage.QueueItem>
    ) : PlaybackQueueStorage {
        private val queueState = MutableStateFlow(queue)
        private val shuffleState = MutableStateFlow(false)
        private val repeatState = MutableStateFlow(PlaybackQueueStorage.RepeatMode.Off)

        var playNextCalls: Int = 0
            private set

        override val currentQueue: Flow<List<PlaybackQueueStorage.QueueItem>> = queueState
        override val shuffleEnabled: Flow<Boolean> = shuffleState
        override val repeatMode: Flow<PlaybackQueueStorage.RepeatMode> = repeatState

        override suspend fun playSongs(items: List<Song.Id>) = Unit
        override suspend fun addSongs(items: List<Song.Id>) = Unit
        override suspend fun setShuffleEnabled(enabled: Boolean) {
            shuffleState.emit(enabled)
        }

        override suspend fun setRepeatMode(mode: PlaybackQueueStorage.RepeatMode) {
            repeatState.emit(mode)
        }

        override suspend fun playNext() {
            playNextCalls++
        }

        override suspend fun playPrev() = Unit
        override suspend fun playSongInQueue(queueId: Long) = Unit
        override suspend fun removeSongsByIds(ids: List<Song.Id>) = Unit
    }

    private class FakePlaybackPlayer(
        state: PlaybackPlayer.State,
    ) : PlaybackPlayer {
        override val progress: Flow<PlaybackPlayer.Progress> = flowOf(PlaybackPlayer.Progress(position = 0, duration = 0))
        private val stateFlow = MutableStateFlow(state)
        override val state: StateFlow<PlaybackPlayer.State> = stateFlow.asStateFlow()

        override suspend fun stop() = Unit
        override suspend fun pause() = Unit
        override suspend fun resume() = Unit
        override suspend fun seekTo(position: Long) = Unit
        override suspend fun setMediaItem(item: MediaItemWrapper, position: Long) = Unit
    }

    private class FakeCatalogSource : CatalogSource {
        override suspend fun getArtists(): List<Artist> = emptyList()
        override suspend fun getArtistById(id: Artist.Id): Artist? = null
        override suspend fun getAlbums(artistId: Artist.Id): List<Album> = emptyList()
        override suspend fun getSongsByArtist(artistId: Artist.Id): List<Song> = emptyList()
        override suspend fun getSongsByAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song> = emptyList()
        override suspend fun getSongsByIds(ids: List<Song.Id>): List<Song> = emptyList()
        override suspend fun getSongById(id: Song.Id): Song? = null
        override suspend fun search(query: String): CatalogSearchResult = CatalogSearchResult(
            artists = emptyList(),
            songs = emptyList(),
        )
        override suspend fun hideSong(id: Song.Id) = Unit
        override suspend fun hideAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song.Id> = emptyList()
        override suspend fun deleteSong(id: Song.Id): Boolean = false
        override suspend fun deleteAlbum(artistId: Artist.Id, albumId: Album.Id): List<Song.Id> = emptyList()
    }
}
