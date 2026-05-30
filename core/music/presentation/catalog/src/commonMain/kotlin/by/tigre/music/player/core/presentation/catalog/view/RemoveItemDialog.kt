package by.tigre.music.player.core.presentation.catalog.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `by`.tigre.music.player.core.presentation.catalog.resources.Res
import `by`.tigre.music.player.core.presentation.catalog.resources.action_cancel
import `by`.tigre.music.player.core.presentation.catalog.resources.action_delete_forever
import `by`.tigre.music.player.core.presentation.catalog.resources.action_hide
import `by`.tigre.music.player.core.presentation.catalog.resources.remove_item_message
import `by`.tigre.music.player.core.presentation.catalog.resources.remove_item_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoveItemDialog(
    onHide: () -> Unit,
    onDeleteForever: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.remove_item_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.remove_item_message))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onHide
                ) {
                    Text(stringResource(Res.string.action_hide))
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeleteForever
                ) {
                    Text(stringResource(Res.string.action_delete_forever))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = onDismiss
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
