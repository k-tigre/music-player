package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.runtime.Composable
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.chapter_fallback_title
import by.tigre.audiobook.core.presentation.catalog.resources.chapter_sheet_subtitle
import org.jetbrains.compose.resources.stringResource

internal fun isNonDescriptiveChapterTitle(title: String, index: Int): Boolean {
    val raw = title.trim()
    if (raw.isEmpty()) return true

    val chapterNumber = index + 1
    if (raw == chapterNumber.toString()) return true
    if (raw == "%02d".format(chapterNumber)) return true
    if (raw == "%03d".format(chapterNumber)) return true
    if (raw.matches(Regex("^\\d{1,4}$"))) return true
    if (raw.length <= 2) return true
    if (raw.matches(Regex("^[\\d._\\-]+$"))) return true

    val lower = raw.lowercase()
    if (lower == "chapter $chapterNumber" || lower == "глава $chapterNumber") return true
    if (lower == "track $chapterNumber" || lower == "трек $chapterNumber") return true

    return false
}

@Composable
internal fun chapterDisplayTitle(title: String, index: Int): String {
    if (index < 0) return title
    return if (isNonDescriptiveChapterTitle(title, index)) {
        stringResource(Res.string.chapter_fallback_title, index + 1)
    } else {
        title
    }
}

@Composable
internal fun chapterSheetSubtitle(
    rawTitle: String,
    index: Int,
    totalChapters: Int,
    offsetMs: Long,
): String {
    val position = stringResource(
        Res.string.chapter_sheet_subtitle,
        index + 1,
        totalChapters,
        formatChapterDuration(offsetMs),
    )
    val raw = rawTitle.trim()
    val showRawHint = isNonDescriptiveChapterTitle(raw, index) &&
        raw.isNotEmpty() &&
        !raw.matches(Regex("^\\d{1,4}$")) &&
        !raw.equals(chapterDisplayTitle(rawTitle, index), ignoreCase = true)

    return if (showRawHint) "$raw · $position" else position
}

internal fun chapterOffsetMs(chapters: List<by.tigre.audiobook.core.entity.catalog.Chapter>, index: Int): Long {
    if (index <= 0) return 0L
    return chapters.take(index).sumOf { it.duration.coerceAtLeast(0L) }
}

internal fun formatChapterDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
