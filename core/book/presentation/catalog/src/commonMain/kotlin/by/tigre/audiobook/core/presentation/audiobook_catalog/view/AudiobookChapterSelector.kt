package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.data.audiobook_playback.AudiobookPlaybackController
import by.tigre.audiobook.core.entity.catalog.Chapter
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.book_currently_playing
import by.tigre.audiobook.core.presentation.catalog.resources.cd_select_chapter
import by.tigre.audiobook.core.presentation.catalog.resources.chapters_sheet_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookChapterSelector(
    controller: AudiobookPlaybackController,
    chapterTitle: String,
    modifier: Modifier = Modifier,
) {
    val chapters by controller.chapters.collectAsState()
    val currentChapter by controller.currentChapter.collectAsState()
    var sheetVisible by remember { mutableStateOf(false) }
    val canSelect = chapters.size > 1
    val currentIndex = chapters.indexOfFirst { it.id == currentChapter?.id }
    val displayTitle = chapterDisplayTitle(chapterTitle, currentIndex)

    Column(
        modifier = modifier
            .clickable(enabled = canSelect) { sheetVisible = true }
            .padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (canSelect) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(Res.string.cd_select_chapter),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        if (currentIndex >= 0 && chapters.isNotEmpty()) {
            Text(
                text = chapterSheetSubtitle(
                    rawTitle = chapterTitle,
                    index = currentIndex,
                    totalChapters = chapters.size,
                    offsetMs = chapterOffsetMs(chapters, currentIndex),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (sheetVisible && canSelect) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            sheetState = sheetState,
        ) {
            AudiobookChapterListSheet(
                controller = controller,
                onChapterSelected = { chapterId ->
                    sheetVisible = false
                    if (chapterId != currentChapter?.id) {
                        controller.jumpToChapter(chapterId)
                    }
                },
            )
        }
    }
}

@Composable
fun AudiobookChapterListSheet(
    controller: AudiobookPlaybackController,
    onChapterSelected: (Chapter.Id) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val chapters by controller.chapters.collectAsState()
    val currentChapter by controller.currentChapter.collectAsState()
    val currentIndex = chapters.indexOfFirst { it.id == currentChapter?.id }
    val listState = rememberLazyListState()

    LaunchedEffect(chapters, currentIndex) {
        if (currentIndex >= 0) {
            listState.scrollToItem(currentIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.chapters_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(
                items = chapters,
                key = { _, chapter -> chapter.id.value },
            ) { index, chapter ->
                ChapterSheetRow(
                    index = index,
                    chapter = chapter,
                    totalChapters = chapters.size,
                    offsetMs = chapterOffsetMs(chapters, index),
                    isCurrent = chapter.id == currentChapter?.id,
                    onClick = { onChapterSelected(chapter.id) },
                )
            }
        }
    }
}

@Composable
private fun ChapterSheetRow(
    index: Int,
    chapter: Chapter,
    totalChapters: Int,
    offsetMs: Long,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val title = chapterDisplayTitle(chapter.title, index)
    val subtitle = chapterSheetSubtitle(
        rawTitle = chapter.title,
        index = index,
        totalChapters = totalChapters,
        offsetMs = offsetMs,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isCurrent) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(width = 24.dp, height = 20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (isCurrent) {
                Text(
                    text = stringResource(Res.string.book_currently_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatChapterDuration(chapter.duration),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
