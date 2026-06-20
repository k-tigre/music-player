package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `by`.tigre.media.platform.tools.platform.compose.resources.Res
import `by`.tigre.media.platform.tools.platform.compose.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardWithPopup(
    modifier: Modifier,
    title: String,
    popupActions: List<PopupAction>? = null,
    cardClickPopupActions: List<PopupAction>? = null,
    onCardClicked: () -> Unit,
    descriptions: List<String>,
) {
    var popupControl by remember { mutableStateOf(false) }
    var activePopupActions by remember { mutableStateOf<List<PopupAction>?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (cardClickPopupActions != null) {
                    activePopupActions = cardClickPopupActions
                    popupControl = true
                } else {
                    onCardClicked()
                }
            },
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    text = title,
                )

                if (popupActions != null) {
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        onClick = {
                            activePopupActions = popupActions
                            popupControl = true
                        }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.cd_more_options)
                        )
                    }
                }
            }

            descriptions.forEach { item ->
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = item
                )
            }

            Spacer(modifier = Modifier.size(8.dp))
        }

        DropdownMenu(
            expanded = popupControl,
            onDismissRequest = { popupControl = false },
        ) {
            activePopupActions?.forEach { (title, action) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = title,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        popupControl = false
                        action()
                    },
                )
            }
        }
    }
}

data class PopupAction(val title: String, val action: () -> Unit)
