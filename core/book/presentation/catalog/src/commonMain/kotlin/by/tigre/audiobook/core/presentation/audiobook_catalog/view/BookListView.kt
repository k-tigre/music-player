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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import by.tigre.audiobook.core.presentation.catalog.resources.cd_manage_folders
import by.tigre.audiobook.core.presentation.catalog.resources.continue_listening_title
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource

class BookListView(
    private val component: BookListComponent
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.audiobooks_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(onClick = component::onManageFolders) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(Res.string.cd_manage_folders)
                            )
                        }
                    }
                )
            },
            content = { paddingValues ->
                val screenState by component.screenState.collectAsState()

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
        if (state.continueListeningBook == null && state.rootBooks.isEmpty() && state.grouped.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(state.scrollToBookNonce) {
                if (state.scrollToBookNonce > 0L) {
                    listState.animateScrollToItem(0)
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                state.continueListeningBook?.let { book ->
                    item(key = "continue_${book.id.value}") {
                        Text(
                            text = stringResource(Res.string.continue_listening_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                        )
                        BookCard(
                            book = book,
                            isCurrent = true,
                            showNowPlayingBadge = true,
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                }

                items(items = state.rootBooks, key = { book -> book.id.value }) { book ->
                    BookCard(
                        book = book,
                        isCurrent = book.id == state.currentBookId,
                    )
                }

                state.grouped.forEach { (path, booksInGroup) ->
                    stickyHeader(key = "header_$path") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.background)
                                .padding(top = 12.dp, bottom = 4.dp)
                                .clickable { component.toggleGroup(path) }
                        ) {
                            Text(
                                text = path.replace("/", " / "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                    if (state.expanded.contains(path)) {
                        items(booksInGroup, key = { book -> "group_${path}_${book.id.value}" }) { book ->
                            BookCard(
                                book = book,
                                isCurrent = book.id == state.currentBookId,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BookCard(
        book: Book,
        isCurrent: Boolean,
        showNowPlayingBadge: Boolean = false,
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
                .clickable { component.onBookClicked(book) },
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cover = book.coverUri
                if (cover != null) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
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
            }
        }
    }
}
