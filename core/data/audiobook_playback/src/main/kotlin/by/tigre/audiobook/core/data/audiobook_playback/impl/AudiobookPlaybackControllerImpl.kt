package by.tigre.audiobook.core.data.audiobook_playback.impl

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import by.tigre.audiobook.core.data.audiobook.AudiobookCatalogSource
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.data.storage.audiobook_playback.AudiobookPlaybackStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.music.player.core.data.playback.PlaybackPlayer
import by.tigre.music.player.logger.Log
import by.tigre.music.player.tools.coroutines.CoreScope
import by.tigre.music.player.tools.coroutines.extensions.tickerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class AudiobookPlaybackControllerImpl(
    override val player: PlaybackPlayer,
    private val catalog: AudiobookCatalogSource,
    private val storage: AudiobookPlaybackStorage,
    private val scope: CoreScope
) : AudiobookPlaybackController {

    override val currentBook = MutableStateFlow<Book?>(null)
    override val currentChapter = MutableStateFlow<Chapter?>(null)

    private val chapters = MutableStateFlow<List<Chapter>>(emptyList())
    private val isPlaying = MutableStateFlow(false)

    init {
        scope.launch {
            player.state
                .filter { it == PlaybackPlayer.State.Ended }
                .collect { playNextChapter() }
        }

        scope.launch {
            isPlaying
                .flatMapLatest { isPlaying ->
                    if (isPlaying) {
                        tickerFlow(5.seconds)
                    } else {
                        emptyFlow()
                    }
                }.collect {
                    saveCurrentPosition()
                }
        }
    }

    override fun playBook(book: Book) {
        Log.d(TAG) { "playBook: ${book.title}" }
        scope.launch {
            val chapterList = catalog.getChapters(book.id)
            if (chapterList.isEmpty()) {
                Log.w(TAG) { "No chapters for book: ${book.title}" }
                return@launch
            }

            chapters.value = chapterList
            currentBook.value = book

            val savedPosition = storage.getPosition(book.id)
            val startChapter = if (savedPosition != null) {
                chapterList.firstOrNull { it.id == savedPosition.chapterId } ?: chapterList.first()
            } else {
                chapterList.first()
            }
            val startPosition = if (savedPosition?.chapterId == startChapter.id) savedPosition.positionMs else 0L

            setChapter(startChapter, startPosition)
            isPlaying.value = true
            player.resume()
        }
    }

    override fun playNextChapter() {
        Log.d(TAG) { "playNextChapter" }
        scope.launch {
            val chapterList = chapters.value
            val current = currentChapter.value ?: return@launch
            val nextIndex = chapterList.indexOfFirst { it.id == current.id } + 1
            if (nextIndex < chapterList.size) {
                setChapter(chapterList[nextIndex], 0L)
                player.resume()
            } else {
                Log.d(TAG) { "Book finished" }
                currentBook.value?.let { book ->
                    storage.savePosition(book.id, chapterList.first().id, 0L)
                }
                isPlaying.value = false
                player.stop()
            }
        }
    }

    override fun playPrevChapter() {
        Log.d(TAG) { "playPrevChapter" }
        scope.launch {
            val chapterList = chapters.value
            val current = currentChapter.value ?: return@launch
            val prevIndex = chapterList.indexOfFirst { it.id == current.id } - 1
            if (prevIndex >= 0) {
                setChapter(chapterList[prevIndex], 0L)
                player.resume()
            } else {
                setChapter(chapterList.first(), 0L)
                player.resume()
            }
        }
    }

    override fun pause() {
        Log.d(TAG) { "pause" }
        scope.launch {
            saveCurrentPosition()
            isPlaying.value = false
            player.pause()
        }
    }

    override fun resume() {
        Log.d(TAG) { "resume" }
        isPlaying.value = true
        scope.launch { player.resume() }
    }

    override fun stop() {
        Log.d(TAG) { "stop" }
        scope.launch {
            saveCurrentPosition()
            isPlaying.value = false
            player.stop()
        }
    }

    private suspend fun setChapter(chapter: Chapter, positionMs: Long) {
        currentChapter.value = chapter
        withContext(Dispatchers.Main) {
            player.player.setMediaItem(
                MediaItem.Builder()
                    .setUri(chapter.fileUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(chapter.title)
                            .setAlbumTitle(currentBook.value?.title)
                            .build()
                    )
                    .build(),
                positionMs
            )
            player.player.prepare()
        }
    }

    private suspend fun saveCurrentPosition() {
        val book = currentBook.value ?: return
        val chapter = currentChapter.value ?: return
        val progress = player.progress.first()
        if (progress.position > 0) {
            storage.savePosition(book.id, chapter.id, progress.position)
            Log.d(TAG) { "Saved position: book=${book.title}, chapter=${chapter.title}, pos=${progress.position}" }
        }
    }

    private companion object {
        const val TAG = "AudiobookPlaybackController"
    }
}
