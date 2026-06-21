package by.tigre.music.player.core.presentation.playlist.library.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.data.playlist.AddToPlaylistRequest
import by.tigre.music.player.core.entiry.playlist.Playlist
import by.tigre.music.player.core.presentation.playlist.library.resources.Res
import by.tigre.music.player.core.presentation.playlist.library.resources.add_to_playlist_create_new
import by.tigre.music.player.core.presentation.playlist.library.resources.playlist_create
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    request: AddToPlaylistRequest?,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist.Id) -> Unit,
    onCreateAndAdd: (name: String) -> Unit,
) {
    if (request == null) return

    var newPlaylistName by remember(request) { mutableStateOf("") }
    val isCreateEnabled = newPlaylistName.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = request.previewText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.add_to_playlist_create_new),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = isCreateEnabled,
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotEmpty()) {
                            onCreateAndAdd(name)
                            newPlaylistName = ""
                        }
                    },
                ) {
                    Text(stringResource(Res.string.playlist_create))
                }
            }

            LazyColumn {
                items(
                    items = playlists,
                    key = { it.id.value },
                ) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(playlist.id) },
                    )
                }
            }
        }
    }
}
