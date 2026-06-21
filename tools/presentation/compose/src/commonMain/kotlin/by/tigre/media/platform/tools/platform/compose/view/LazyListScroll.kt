package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameMillis
import kotlin.math.abs
import kotlin.math.max

suspend fun LazyListState.smartScrollToItem(
    targetIndex: Int,
    nearMarginPx: Float,
    smoothDurationMillis: Int = 1000,
    instant: Boolean = false,
) {
    if (targetIndex < 0 || !layoutInfo.hasValidViewport) return

    val isFar = isTargetFarFromViewport(
        targetIndex = targetIndex,
        layoutInfo = layoutInfo,
    )

    when {
        isFar -> scrollFar(
            targetIndex = targetIndex,
            nearMarginPx = nearMarginPx,
            instant = instant,
            smoothDurationMillis = smoothDurationMillis,
        )
        else -> scrollNear(
            targetIndex = targetIndex,
            nearMarginPx = nearMarginPx,
            instant = instant,
            smoothDurationMillis = smoothDurationMillis,
        )
    }
}

private suspend fun LazyListState.scrollFar(
    targetIndex: Int,
    nearMarginPx: Float,
    instant: Boolean,
    smoothDurationMillis: Int,
) {
    val layoutInfo = layoutInfo
    val centerOffset = layoutInfo.estimateCenterScrollOffset(nearMarginPx)

    if (instant) {
        scrollToItem(targetIndex, scrollOffset = centerOffset)
    } else {
        animateScrollToItem(
            index = targetIndex,
            scrollOffset = centerOffset,
        )
    }

    awaitTargetLayout(targetIndex)
    adjustToCenter(
        targetIndex = targetIndex,
        nearMarginPx = nearMarginPx,
        instant = true,
    )
}

private suspend fun LazyListState.scrollNear(
    targetIndex: Int,
    nearMarginPx: Float,
    instant: Boolean,
    smoothDurationMillis: Int,
) {
    val layoutInfo = layoutInfo
    val isVisible = layoutInfo.visibleItemsInfo.any { it.index == targetIndex }

    if (!isVisible) {
        val nearOffset = layoutInfo.estimateNearScrollOffset(targetIndex, nearMarginPx)
        if (instant) {
            scrollToItem(targetIndex, scrollOffset = nearOffset)
        } else {
            animateScrollToItem(targetIndex, scrollOffset = nearOffset)
        }
        awaitTargetLayout(targetIndex)
    }

    adjustToNearMargin(
        targetIndex = targetIndex,
        nearMarginPx = nearMarginPx,
        instant = instant,
        smoothDurationMillis = smoothDurationMillis,
    )
}

private suspend fun LazyListState.adjustToCenter(
    targetIndex: Int,
    nearMarginPx: Float,
    instant: Boolean,
) {
    val layoutInfo = layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex } ?: return

    val bounds = layoutInfo.usableBounds(nearMarginPx)
    val itemHeight = visibleItem.size.toFloat()
    val idealTop = coerceIdealTop(
        idealTop = bounds.top + (bounds.height - itemHeight) / 2f,
        itemHeight = itemHeight,
        bounds = bounds,
    )

    applyScrollDelta(
        delta = visibleItem.offset.toFloat() - idealTop,
        instant = instant,
        smoothDurationMillis = 0,
    )
}

private suspend fun LazyListState.adjustToNearMargin(
    targetIndex: Int,
    nearMarginPx: Float,
    instant: Boolean,
    smoothDurationMillis: Int,
) {
    val layoutInfo = layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == targetIndex } ?: return

    val bounds = layoutInfo.usableBounds(nearMarginPx)
    val itemHeight = visibleItem.size.toFloat()
    val itemTop = visibleItem.offset.toFloat()
    val itemBottom = itemTop + itemHeight

    val idealTop = nearIdealTop(
        itemTop = itemTop,
        itemBottom = itemBottom,
        itemHeight = itemHeight,
        bounds = bounds,
    ) ?: return

    applyScrollDelta(
        delta = itemTop - coerceIdealTop(idealTop, itemHeight, bounds),
        instant = instant,
        smoothDurationMillis = smoothDurationMillis,
    )
}

private suspend fun LazyListState.applyScrollDelta(
    delta: Float,
    instant: Boolean,
    smoothDurationMillis: Int,
) {
    if (abs(delta) < 1f) return

    if (instant || smoothDurationMillis <= 0) {
        scrollBy(delta)
    } else {
        animateScrollBy(
            value = delta,
            animationSpec = tween(durationMillis = smoothDurationMillis),
        )
    }
}

private fun LazyListState.isTargetFarFromViewport(
    targetIndex: Int,
    layoutInfo: LazyListLayoutInfo,
): Boolean {
    if (layoutInfo.visibleItemsInfo.any { it.index == targetIndex }) return false

    val viewportHeight = layoutInfo.viewportHeight
    val averageItemSize = layoutInfo.averageItemSize(viewportHeight)
    val firstVisible = firstVisibleItemIndex
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
    val itemDistance = when {
        targetIndex < firstVisible -> firstVisible - targetIndex
        targetIndex > lastVisible -> targetIndex - lastVisible
        else -> 0
    }
    return itemDistance * averageItemSize > viewportHeight
}

private fun LazyListLayoutInfo.estimateCenterScrollOffset(nearMarginPx: Float): Int {
    val bounds = usableBounds(nearMarginPx)
    val avgItemSize = averageItemSize(viewportHeight)
    val idealTop = bounds.top + (bounds.height - avgItemSize) / 2f
    return scrollOffsetForItemTop(idealTop)
}

private fun LazyListLayoutInfo.estimateNearScrollOffset(
    targetIndex: Int,
    nearMarginPx: Float,
): Int {
    val bounds = usableBounds(nearMarginPx)
    val avgItemSize = averageItemSize(viewportHeight)
    val lastVisibleIndex = visibleItemsInfo.lastOrNull()?.index ?: 0
    val idealTop = if (targetIndex > lastVisibleIndex) {
        bounds.bottom - avgItemSize
    } else {
        bounds.top
    }
    return scrollOffsetForItemTop(idealTop)
}

/**
 * [LazyListState.scrollToItem] / [LazyListState.animateScrollToItem] place the item at the top
 * of the viewport first; [scrollOffset] is applied forward (positive = item moves up, off-screen).
 * To show the item at [idealItemTop] px from the viewport start, the offset must be negative.
 */
private fun LazyListLayoutInfo.scrollOffsetForItemTop(idealItemTop: Float): Int =
    (beforeContentPadding - idealItemTop).toInt()

private data class UsableViewportBounds(
    val top: Float,
    val bottom: Float,
    val height: Float,
)

private val LazyListLayoutInfo.hasValidViewport: Boolean
    get() = viewportHeight > 0f

private val LazyListLayoutInfo.viewportHeight: Float
    get() = (viewportEndOffset - viewportStartOffset).toFloat()

private fun LazyListLayoutInfo.averageItemSize(viewportHeight: Float): Float =
    visibleItemsInfo
        .map { it.size }
        .average()
        .takeIf { !it.isNaN() && it > 0 }
        ?.toFloat()
        ?: (viewportHeight / 5f)

private fun LazyListLayoutInfo.usableBounds(nearMarginPx: Float): UsableViewportBounds {
    val viewportHeight = viewportHeight
    val bottomInset = afterContentPadding.toFloat()
    val topInset = beforeContentPadding.toFloat()
    val top = max(nearMarginPx, topInset)
    val bottom = viewportHeight - bottomInset - nearMarginPx
    return UsableViewportBounds(
        top = top,
        bottom = bottom,
        height = (bottom - top).coerceAtLeast(0f),
    )
}

private fun nearIdealTop(
    itemTop: Float,
    itemBottom: Float,
    itemHeight: Float,
    bounds: UsableViewportBounds,
): Float? {
    val topMarginOk = itemTop >= bounds.top
    val bottomMarginOk = itemBottom <= bounds.bottom
    if (topMarginOk && bottomMarginOk) return null

    return when {
        !bottomMarginOk -> bounds.bottom - itemHeight
        !topMarginOk -> bounds.top
        else -> null
    }
}

private fun coerceIdealTop(
    idealTop: Float,
    itemHeight: Float,
    bounds: UsableViewportBounds,
): Float {
    val maxTop = (bounds.bottom - itemHeight).coerceAtLeast(bounds.top)
    return idealTop.coerceIn(bounds.top, maxTop)
}

private suspend fun LazyListState.awaitTargetLayout(targetIndex: Int, maxFrames: Int = 10) {
    repeat(maxFrames) {
        if (layoutInfo.visibleItemsInfo.any { it.index == targetIndex }) return
        withFrameMillis { }
    }
}
