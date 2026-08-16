package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.entity.catalog.Book
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.BookListComponent
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.audiobooks_empty_hint
import by.tigre.audiobook.core.presentation.catalog.resources.audiobooks_empty_title
import by.tigre.audiobook.core.presentation.catalog.resources.audiobooks_title
import by.tigre.audiobook.core.presentation.catalog.resources.book_chapters_count
import by.tigre.audiobook.core.presentation.catalog.resources.book_completed
import by.tigre.audiobook.core.presentation.catalog.resources.book_currently_playing
import by.tigre.audiobook.core.presentation.catalog.resources.book_progress_listened
import by.tigre.audiobook.core.presentation.catalog.resources.cd_collapse_folder
import by.tigre.audiobook.core.presentation.catalog.resources.cd_dismiss_continue_listening
import by.tigre.audiobook.core.presentation.catalog.resources.cd_expand_folder
import by.tigre.audiobook.core.presentation.catalog.resources.cd_open_settings
import by.tigre.audiobook.core.presentation.catalog.resources.continue_listening_books_count
import by.tigre.audiobook.core.presentation.catalog.resources.continue_listening_show_more
import by.tigre.audiobook.core.presentation.catalog.resources.continue_listening_title
import by.tigre.audiobook.core.presentation.catalog.resources.folder_group_books_count
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_add_books
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_add_folder
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_add_selected
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create_upsell
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create_upsell_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_edit_cd
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_edit_done
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_edit_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_edit_title
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_empty_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_empty_title
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_kids_suggestion
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_new_name_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_picker_empty
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_picker_title
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_remove_cd
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_root_folder
import by.tigre.audiobook.core.presentation.catalog.resources.library_spaces_sheet_title
import by.tigre.media.platform.presentation.ScreenContentState
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.appTopBarWindowInsets
import by.tigre.media.platform.tools.platform.compose.view.ErrorScreen
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicator
import by.tigre.media.platform.tools.platform.compose.view.ProgressIndicatorSize
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail
import by.tigre.media.platform.tools.platform.compose.view.bottomBarListContentPadding
import by.tigre.media.platform.tools.platform.compose.view.centeredScreenContentBottomPadding
import org.jetbrains.compose.resources.stringResource

class BookListView(
    private val component: BookListComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val screenState by component.screenState.collectAsState()
        val content = (screenState as? ScreenContentState.Content)?.value
        val editMembership = content?.editMembership == true

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                if (editMembership) Res.string.library_space_edit_title
                                else Res.string.audiobooks_title,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    windowInsets = appTopBarWindowInsets(),
                    navigationIcon = {
                        if (content?.spacesVisible == true && content.activeSpace != null) {
                            Text(
                                text = content.activeSpace.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable(
                                        enabled = !editMembership,
                                        onClick = component::onSpaceChipClicked,
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        if (content?.spacesVisible == true) {
                            if (editMembership) {
                                TextButton(onClick = component::onDoneEditMembership) {
                                    Text(stringResource(Res.string.library_space_edit_done))
                                }
                            } else {
                                IconButton(onClick = component::onEditMembershipClicked) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = stringResource(
                                            Res.string.library_space_edit_cd,
                                        ),
                                    )
                                }
                            }
                        }
                        if (!editMembership) {
                            IconButton(onClick = component::onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(Res.string.cd_open_settings)
                                )
                            }
                        }
                    }
                )
            },
            content = { paddingValues ->
                val spaceSheetVisible by component.spaceSheetVisible.collectAsState()
                val addBooksSheetVisible by component.addBooksSheetVisible.collectAsState()

                if (spaceSheetVisible) {
                    val sheetContent = (screenState as? ScreenContentState.Content)?.value
                    if (sheetContent != null) {
                        SpaceSwitcherSheet(sheetContent)
                    }
                }
                if (addBooksSheetVisible) {
                    AddBooksSheet()
                }

                AnimatedContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    targetState = screenState,
                    label = "state",
                    contentKey = { state -> state::class.java },
                ) { state ->
                    when (state) {
                        is ScreenContentState.Loading -> {
                            ProgressIndicator(Modifier.fillMaxSize(), ProgressIndicatorSize.LARGE)
                        }

                        is ScreenContentState.Error -> {
                            ErrorScreen(retryAction = component::retry)
                        }

                        is ScreenContentState.Content -> {
                            DrawContent(state.value)
                        }
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DrawContent(state: BookListComponent.BookListUiState) {
        LaunchedEffect(Unit) {
            component.onScreenShown()
        }

        if (state.continueListeningBooks.isEmpty() && state.rootBooks.isEmpty() && state.grouped.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .centeredScreenContentBottomPadding()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.emptySpaceNeedsBooks) {
                    Text(
                        text = stringResource(Res.string.library_space_empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(Res.string.library_space_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.library_space_add_books),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = component::onAddBooksClicked),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.audiobooks_empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(Res.string.audiobooks_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

            val listState = rememberLazyListState()
            LaunchedEffect(state.scrollToBookNonce) {
                if (state.scrollToBookNonce > 0L) {
                    listState.animateScrollToItem(0)
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = bottomBarListContentPadding(),
            ) {
                if (state.editMembership) {
                    item(key = "edit_hint") {
                        Text(
                            text = stringResource(Res.string.library_space_edit_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                if (!state.editMembership && state.continueListeningBooks.isNotEmpty()) {
                    stickyHeader(key = "continue_header") {
                        ContinueListeningHeader(
                            bookCount = state.continueListeningTotalCount,
                            isExpanded = state.continueListeningExpanded,
                            onClick = component::toggleContinueListening,
                        )
                    }
                    if (state.continueListeningExpanded) {
                        items(
                            items = state.continueListeningBooks,
                            key = { book -> "continue_${book.id.value}" },
                        ) { book ->
                            CompactContinueBookCard(
                                book = book,
                                isCurrent = book.id == state.currentBookId,
                                onDismiss = { component.dismissContinueListening(book) },
                            )
                        }
                        if (state.continueListeningHasMore) {
                            item(key = "continue_show_more") {
                                Text(
                                    text = stringResource(Res.string.continue_listening_show_more),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = component::requestMoreContinueListening)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }
                    item(key = "continue_spacer") {
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                }

                items(items = state.rootBooks, key = { book -> book.id.value }) { book ->
                    BookCard(
                        book = book,
                        isCurrent = book.id == state.currentBookId,
                        editMembership = state.editMembership,
                        onRemoveFromSpace = {
                            component.onRemoveBookFromSpace(book.id)
                        },
                    )
                }

                state.grouped.forEach { (path, booksInGroup) ->
                    val isExpanded = state.editMembership || state.expanded.contains(path)
                    val booksToShow = if (isExpanded) {
                        booksInGroup
                    } else {
                        booksInGroup.filter { book -> book.id == state.currentBookId }
                    }
                    stickyHeader(key = "header_$path") {
                        FolderGroupHeader(
                            path = path,
                            bookCount = booksInGroup.size,
                            isExpanded = isExpanded,
                            editMembership = state.editMembership,
                            onClick = { component.toggleGroup(path) },
                            onRemoveFromSpace = {
                                component.onRemoveFolderFromSpace(path)
                            },
                        )
                    }
                    if (booksToShow.isNotEmpty()) {
                        items(booksToShow, key = { book -> "group_${path}_${book.id.value}" }) { book ->
                            BookCard(
                                book = book,
                                isCurrent = book.id == state.currentBookId,
                                editMembership = state.editMembership,
                                onRemoveFromSpace = {
                                    component.onRemoveBookFromSpace(book.id)
                                },
                            )
                        }
                    }
                }
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SpaceSwitcherSheet(state: BookListComponent.BookListUiState) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var creating by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = component::dismissSpaceSheet,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.library_spaces_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                state.spaces.forEach { space ->
                    val selected = space.id == state.activeSpace?.id
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { component.onSpaceSelected(space.id) }
                            .padding(vertical = 12.dp),
                    )
                }
                if (creating && state.canManageSpaces) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(stringResource(Res.string.library_space_new_name_hint)) },
                        placeholder = { Text(stringResource(Res.string.library_space_kids_suggestion)) },
                        singleLine = true,
                    )
                    val kidsSuggestion = stringResource(Res.string.library_space_kids_suggestion)
                    TextButton(
                        onClick = {
                            component.onConfirmCreateSpace(name.ifBlank { kidsSuggestion })
                            creating = false
                        },
                    ) {
                        Text(stringResource(Res.string.library_space_create))
                    }
                } else {
                    TextButton(
                        onClick = {
                            if (state.canManageSpaces) {
                                creating = true
                                name = ""
                            } else {
                                component.onCreateSpaceClicked()
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.canManageSpaces) Res.string.library_space_create
                                else Res.string.library_space_create_upsell,
                            ),
                        )
                    }
                    if (!state.canManageSpaces) {
                        Text(
                            text = stringResource(Res.string.library_space_create_upsell_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddBooksSheet() {
        val picker by component.addBooksPicker.collectAsState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = component::dismissAddBooksSheet,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.library_space_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (picker.candidates.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.library_space_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    ) {
                        if (picker.folders.isNotEmpty()) {
                            item(key = "folders_header") {
                                Text(
                                    text = stringResource(Res.string.library_space_add_folder),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                                )
                            }
                            items(
                                items = picker.folders,
                                key = { (path, _) -> "folder:$path" },
                            ) { (path, count) ->
                                val label = if (path.isEmpty()) {
                                    stringResource(Res.string.library_space_root_folder)
                                } else {
                                    path
                                }
                                Text(
                                    text = "$label ($count)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { component.addPickerFolder(path) }
                                        .padding(vertical = 8.dp),
                                )
                            }
                        }
                        items(
                            items = picker.candidates,
                            key = { it.id.value },
                        ) { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { component.togglePickerBook(book.id) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = book.id in picker.selectedIds,
                                    onCheckedChange = { component.togglePickerBook(book.id) },
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(text = book.title, style = MaterialTheme.typography.bodyLarge)
                                    if (book.subPath.isNotEmpty()) {
                                        Text(
                                            text = book.subPath,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = component::confirmAddSelectedBooks,
                        enabled = picker.selectedIds.isNotEmpty(),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.library_space_add_selected,
                                picker.selectedIds.size,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ContinueListeningHeader(
        bookCount: Int,
        isExpanded: Boolean,
        onClick: () -> Unit,
    ) {
        val shape = RoundedCornerShape(8.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 4.dp, bottom = 2.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.continue_listening_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(Res.string.continue_listening_books_count, bookCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (isExpanded) Res.string.cd_collapse_folder else Res.string.cd_expand_folder,
                        ),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactContinueBookCard(
        book: Book,
        isCurrent: Boolean,
        onDismiss: () -> Unit,
    ) {
        val shape = RoundedCornerShape(8.dp)
        val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        val containerColor = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .border(
                    width = if (isCurrent) 1.dp else 0.5.dp,
                    color = borderColor,
                    shape = shape,
                )
                .clickable { component.onBookClicked(book) },
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverThumbnail(
                    model = book.coverUri,
                    size = 44.dp,
                    cornerRadius = 6.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isCurrent) {
                        Text(
                            text = stringResource(Res.string.book_currently_playing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 1.dp),
                        )
                    }
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.book_chapters_count, book.chapterCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (book.progressFraction > 0f) {
                            Text(
                                text = stringResource(
                                    Res.string.book_progress_listened,
                                    (book.progressFraction * 100).toInt(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (book.progressFraction > 0f) {
                        LinearProgressIndicator(
                            progress = { book.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cd_dismiss_continue_listening),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    @Composable
    private fun FolderGroupHeader(
        path: String,
        bookCount: Int,
        isExpanded: Boolean,
        editMembership: Boolean,
        onClick: () -> Unit,
        onRemoveFromSpace: () -> Unit,
    ) {
        val shape = RoundedCornerShape(12.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 8.dp, bottom = 4.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = shape,
                    )
                    .clickable(enabled = !editMembership, onClick = onClick),
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editMembership) {
                        IconButton(
                            onClick = onRemoveFromSpace,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = stringResource(Res.string.library_space_remove_cd),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = path.replace("/", " / "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(Res.string.folder_group_books_count, bookCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!editMembership) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (isExpanded) Res.string.cd_collapse_folder else Res.string.cd_expand_folder,
                            ),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BookCard(
        book: Book,
        isCurrent: Boolean,
        editMembership: Boolean = false,
        onRemoveFromSpace: (() -> Unit)? = null,
        showNowPlayingBadge: Boolean = false,
        onDismiss: (() -> Unit)? = null,
    ) {
        val shape = RoundedCornerShape(12.dp)
        val borderColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        val containerColor = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .animateContentSize()
                .border(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                )
                .clickable(enabled = !editMembership) { component.onBookClicked(book) },
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editMembership && onRemoveFromSpace != null) {
                    IconButton(
                        onClick = onRemoveFromSpace,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(Res.string.library_space_remove_cd),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                CoverThumbnail(
                    model = book.coverUri,
                    size = 64.dp,
                    cornerRadius = 8.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (showNowPlayingBadge) {
                        Text(
                            text = stringResource(Res.string.book_currently_playing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.book_chapters_count, book.chapterCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (book.isCompleted) {
                            Text(
                                text = stringResource(Res.string.book_completed),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (book.progressFraction > 0f) {
                            Text(
                                text = stringResource(
                                    Res.string.book_progress_listened,
                                    (book.progressFraction * 100).toInt(),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (book.progressFraction > 0f || book.isCompleted) {
                        LinearProgressIndicator(
                            progress = { if (book.isCompleted) 1f else book.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.cd_dismiss_continue_listening),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
