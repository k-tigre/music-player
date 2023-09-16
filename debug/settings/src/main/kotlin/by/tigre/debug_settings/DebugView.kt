@file:OptIn(ExperimentalFoundationApi::class)

package by.tigre.debug_settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import by.tigre.music.player.tools.platform.compose.ComposableView
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
internal class DebugView(private val component: DebugComponent) : ComposableView {
    @Composable
    override fun Draw(modifier: Modifier) {
        Column(modifier) {
            val pagerState = rememberPagerState { component.pages.size }

            ScrollableTabRow(
                // Our selected tab is our current page
                selectedTabIndex = pagerState.currentPage,
                // Override the indicator, using the provided pagerTabIndicatorOffset modifier
//                indicator = { tabPositions ->
//                    TabRowDefaults.Indicator(
//                        Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
//                    )
//                }
            ) {
                val scope = rememberCoroutineScope()
                // Add tabs for all of our pages
                component.pages.forEachIndexed { index, page ->
                    Tab(
                        text = { Text(page.title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
            ) { page ->
                when (val component = component.pages[page]) {
                    is DebugLogsComponent -> {
                        DebugLogsView(component).Draw(Modifier)
                    }
                }
            }
        }
    }
}
