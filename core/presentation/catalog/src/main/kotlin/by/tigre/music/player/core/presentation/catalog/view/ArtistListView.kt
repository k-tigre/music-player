package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.component.ArtistListComponent
import by.tigre.music.player.core.presentation.catalog.entiry.Artist
import by.tigre.music.player.presentation.base.ScreenContentState
import by.tigre.music.player.tools.platform.compose.ComposableView
import by.tigre.music.player.tools.platform.compose.view.ErrorScreen
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicator
import by.tigre.music.player.tools.platform.compose.view.ProgressIndicatorSize

class ArtistListView(
    private val component: ArtistListComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Draw() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val screenState by component.screenState.collectAsState()

            when (screenState) {
                is ScreenContentState.Loading -> {
                    ProgressIndicator(Modifier.align(Alignment.Center), ProgressIndicatorSize.LARGE)
                }
                is ScreenContentState.Error -> {
                    ErrorScreen(retryAction = component::retry)
                }
                is ScreenContentState.Content -> {
                    val categories = (screenState as ScreenContentState.Content<List<Artist>>).value
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { artist ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { component.onArtistClicked(artist) },
                                ) {
                                    Text(
                                        text = artist.name,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
