package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.data.catalog.ArtistArt
import by.tigre.music.player.core.data.catalog.ArtistArtModel
import by.tigre.music.player.core.data.catalog.ArtistArtProvider
import by.tigre.music.player.core.entiry.catalog.Artist
import by.tigre.media.platform.tools.platform.compose.view.CoverThumbnail

@Composable
fun ArtistArtThumbnail(
    artistId: Artist.Id,
    name: String,
    artistArtProvider: ArtistArtProvider,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val art by artistArtProvider.observe(artistId, name).collectAsState(ArtistArt.Idle)
    LaunchedEffect(artistId, name) {
        artistArtProvider.request(artistId, name)
    }
    val model = when (val current = art) {
        is ArtistArt.Ready -> when (val m = current.model) {
            is ArtistArtModel.File -> m.path
            is ArtistArtModel.Url -> m.url
        }
        ArtistArt.Idle, ArtistArt.Loading, ArtistArt.Missing -> null
    }
    CoverThumbnail(
        model = model,
        modifier = modifier,
        size = size,
        fallbackIcon = Icons.Outlined.Person,
    )
}
