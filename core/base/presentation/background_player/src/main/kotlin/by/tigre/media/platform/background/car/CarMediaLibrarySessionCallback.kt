package by.tigre.media.platform.background.car

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
internal class CarMediaLibrarySessionCallback(
    private val scope: CoroutineScope,
    private val carMediaLibrary: CarMediaLibrary,
    private val carSessionMediaType: Int,
) : MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            .buildUpon()
            .add(SessionCommand(CarBrowseActions.ADD_TO_QUEUE, Bundle.EMPTY))
            .build()
        return MediaSession.ConnectionResult.accept(
            sessionCommands,
            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
        )
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootExtras = Bundle().apply {
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            )
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            )
        }
        val libraryParams = MediaLibraryService.LibraryParams.Builder()
            .setExtras(rootExtras)
            .build()
        return Futures.immediateFuture(
            LibraryResult.ofItem(CarMediaItemFactory.rootItem(), libraryParams)
        )
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        val future = SettableFuture.create<LibraryResult<Void>>()
        scope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    carMediaLibrary.search(query)
                }
                session.notifySearchResultChanged(browser, query, results.size, params)
                future.set(LibraryResult.ofVoid())
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch {
            try {
                val browseItems = withContext(Dispatchers.IO) {
                    carMediaLibrary.search(query)
                }
                val from = (page * pageSize).coerceAtMost(browseItems.size)
                val to = (from + pageSize).coerceAtMost(browseItems.size)
                val pageItems = browseItems.subList(from, to).map {
                    CarMediaItemFactory.fromBrowseItem(it, carSessionMediaType)
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), null))
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return loadChildren(parentId)
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()
        scope.launch {
            try {
                val item = withContext(Dispatchers.IO) {
                    carMediaLibrary.getBrowseItem(mediaId)
                }
                if (item != null) {
                    future.set(
                        LibraryResult.ofItem(
                            CarMediaItemFactory.fromBrowseItem(item, carSessionMediaType),
                            null,
                        )
                    )
                } else {
                    future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                }
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction != CarBrowseActions.ADD_TO_QUEUE) {
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
        val mediaId = args.getString(MediaConstants.EXTRA_KEY_MEDIA_ID)
            ?: return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
        return try {
            carMediaLibrary.addMediaIdToQueue(mediaId)
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } catch (e: Exception) {
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        playFromMediaItems(mediaItems)
        return Futures.immediateFuture(mediaItems)
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        playFromMediaItems(mediaItems)
        return Futures.immediateFuture(
            MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
        )
    }

    private fun playFromMediaItems(mediaItems: List<MediaItem>) {
        val ids = mediaItems.map { it.mediaId }
        if (ids.size == 1) {
            carMediaLibrary.playMediaId(ids.first())
        } else if (ids.isNotEmpty()) {
            carMediaLibrary.playMediaIds(ids)
        }
    }

    private fun loadChildren(parentId: String): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        scope.launch {
            try {
                val browseItems = withContext(Dispatchers.IO) {
                    carMediaLibrary.getChildren(parentId)
                }
                val mediaItems = browseItems.map { CarMediaItemFactory.fromBrowseItem(it, carSessionMediaType) }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), null))
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
    }
}
