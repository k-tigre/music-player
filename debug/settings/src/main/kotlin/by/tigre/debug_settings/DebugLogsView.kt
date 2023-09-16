package by.tigre.debug_settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import by.tigre.music.player.tools.platform.compose.ComposableView
import bytigremusicplayerloggerdb.Logs
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
internal class DebugLogsView(private val component: DebugLogsComponent) : ComposableView {
    private val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss.SSS", Locale.US)

    @Composable
    override fun Draw(modifier: Modifier) {
        val logs = component.logs.collectAsState(emptyList(), Dispatchers.Main)
        val isRefreshing by component.loading.collectAsState(Dispatchers.Main)

        val pullRefreshState = rememberPullRefreshState(isRefreshing, { component.onRefresh() })

        Box(
            Modifier.pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = logs.value,
                    key = { it.id },
                    itemContent = { log: Logs ->
                        Card {
                            var isExpanded by remember(log.id) { mutableStateOf(false) }

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = when (log.level) {
                                            "WARN" -> Color.Yellow
                                            "ERROR" -> Color.Red
                                            else -> Color.White
                                        }.copy(alpha = 0.5f)
                                    )
                                    .clickable { isExpanded = isExpanded.not() }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = format.format(log.timestemp),
                                    style = MaterialTheme.typography.titleMedium,
                                )

                                Text(
                                    text = "Tag: ${log.tag}",
                                    style = MaterialTheme.typography.labelMedium,
                                )

                                Text(
                                    text = "Thread: ${log.thread ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                )

                                Text(
                                    text = "Message: ${log.message ?: ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                if (isExpanded) {
                                    Text(
                                        text = log.otherFields ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = log.stacktrace ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                )
            }

            PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}
