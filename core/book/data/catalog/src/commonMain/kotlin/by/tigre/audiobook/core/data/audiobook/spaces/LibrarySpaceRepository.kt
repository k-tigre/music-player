package by.tigre.audiobook.core.data.audiobook.spaces

import by.tigre.audiobook.core.data.storage.audiobook_catalog.AudiobookCatalogStorage
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import by.tigre.media.platform.entitlements.EntitlementsRepository
import by.tigre.media.platform.entitlements.Feature
import by.tigre.media.platform.entitlements.FeatureAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LibrarySpaceRepository {
    val activeSpaceId: StateFlow<LibrarySpace.Id>
    val spaces: StateFlow<List<LibrarySpace>>
    val activeSpace: StateFlow<LibrarySpace?>

    suspend fun ensureInitialized()
    fun setActiveSpace(id: LibrarySpace.Id)
    suspend fun refreshSpaces()
    suspend fun createSpace(name: String, icon: String = "folder"): LibrarySpace.Id?
    suspend fun renameSpace(id: LibrarySpace.Id, name: String, icon: String)
    suspend fun deleteSpace(id: LibrarySpace.Id)
    suspend fun addBooks(spaceId: LibrarySpace.Id, bookIds: List<Book.Id>)
    suspend fun addBooksBySubPath(spaceId: LibrarySpace.Id, subPath: String)
    suspend fun removeBook(spaceId: LibrarySpace.Id, bookId: Book.Id)
    suspend fun getBooksGlobal(): List<Book>
}

class LibrarySpaceRepositoryImpl(
    private val storage: AudiobookCatalogStorage,
    private val preferences: LibrarySpacePreferences,
    private val entitlements: EntitlementsRepository,
) : LibrarySpaceRepository {

    private val mutex = Mutex()
    private val _activeSpaceId = MutableStateFlow(LibrarySpace.Id(0))
    private val _spaces = MutableStateFlow<List<LibrarySpace>>(emptyList())
    private val _activeSpace = MutableStateFlow<LibrarySpace?>(null)

    override val activeSpaceId: StateFlow<LibrarySpace.Id> = _activeSpaceId.asStateFlow()
    override val spaces: StateFlow<List<LibrarySpace>> = _spaces.asStateFlow()
    override val activeSpace: StateFlow<LibrarySpace?> = _activeSpace.asStateFlow()

    override suspend fun ensureInitialized() = mutex.withLock {
        val defaultId = storage.getDefaultSpaceId()
        refreshSpacesLocked()
        val saved = preferences.loadActiveSpaceId()?.let { LibrarySpace.Id(it) }
        val resolved = when {
            saved != null && _spaces.value.any { it.id == saved } && canUseSpace(saved) -> saved
            else -> defaultId
        }
        setActiveLocked(resolved)
    }

    override fun setActiveSpace(id: LibrarySpace.Id) {
        if (!canUseSpace(id)) return
        if (_spaces.value.none { it.id == id }) return
        _activeSpaceId.value = id
        _activeSpace.value = _spaces.value.firstOrNull { it.id == id }
        preferences.saveActiveSpaceId(id.value)
    }

    override suspend fun refreshSpaces() = mutex.withLock { refreshSpacesLocked() }

    override suspend fun createSpace(name: String, icon: String): LibrarySpace.Id? = mutex.withLock {
        if (entitlements.access(Feature.BookSpaces) != FeatureAccess.Allowed) return null
        if (storage.countSpaces() >= entitlements.spacesMax()) return null
        val id = storage.createSpace(name = name.trim().ifEmpty { "Space" }, icon = icon, sortOrder = _spaces.value.size)
        refreshSpacesLocked()
        id
    }

    override suspend fun renameSpace(id: LibrarySpace.Id, name: String, icon: String) {
        val existing = storage.getSpace(id) ?: return
        storage.updateSpace(id, name.trim().ifEmpty { existing.name }, icon, existing.sortOrder)
        refreshSpaces()
        if (_activeSpaceId.value == id) {
            _activeSpace.value = storage.getSpace(id)
        }
    }

    override suspend fun deleteSpace(id: LibrarySpace.Id) {
        val space = storage.getSpace(id) ?: return
        if (space.isDefault) return
        storage.deleteSpace(id)
        refreshSpaces()
        if (_activeSpaceId.value == id) {
            setActiveSpace(storage.getDefaultSpaceId())
        }
    }

    override suspend fun addBooks(spaceId: LibrarySpace.Id, bookIds: List<Book.Id>) {
        storage.addBooksToSpace(spaceId, bookIds)
    }

    override suspend fun addBooksBySubPath(spaceId: LibrarySpace.Id, subPath: String) {
        storage.addBooksBySubPathToSpace(spaceId, subPath)
    }

    override suspend fun removeBook(spaceId: LibrarySpace.Id, bookId: Book.Id) {
        storage.removeBookFromSpace(spaceId, bookId)
    }

    override suspend fun getBooksGlobal(): List<Book> = storage.getBooksGlobal()

    private suspend fun refreshSpacesLocked() {
        _spaces.value = storage.getSpaces()
        _activeSpace.value = _spaces.value.firstOrNull { it.id == _activeSpaceId.value }
    }

    private fun setActiveLocked(id: LibrarySpace.Id) {
        _activeSpaceId.value = id
        _activeSpace.value = _spaces.value.firstOrNull { it.id == id }
        preferences.saveActiveSpaceId(id.value)
    }

    private fun canUseSpace(id: LibrarySpace.Id): Boolean {
        val space = _spaces.value.firstOrNull { it.id == id } ?: return false
        if (space.isDefault) return true
        if (entitlements.access(Feature.BookSpaces) != FeatureAccess.Allowed) return false
        return entitlements.spacesMax() > 1
    }
}
