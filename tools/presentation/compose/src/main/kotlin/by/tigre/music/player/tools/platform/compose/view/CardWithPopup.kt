package by.tigre.music.player.tools.platform.compose.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import by.tigre.music.player.tools.platform.compose.AppTheme
import by.tigre.music.playercompose.R

@Composable
fun CardWithPopup(
    modifier: Modifier,
    title: String,
    popupActions: List<PopupAction>? = null,
    onCardClicked: () -> Unit,
    descriptions: List<String>,
) {
    var popupControl by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onCardClicked,
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
                    onClick = { popupControl = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_more_vert_24),
                        contentDescription = "more"
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

        if (popupControl && popupActions != null) {
            Popup(
                alignment = Alignment.CenterEnd,
                onDismissRequest = { popupControl = false },
                offset = IntOffset(-20, 0),
                properties = PopupProperties()
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.medium)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.medium
                        ),
                )
                {
                    popupActions.forEach { (title, action) ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                popupControl = false
                                action()
                            },
                        ) {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PopupAction(val title: String, val action: () -> Unit)

@Preview
@Composable
private fun Preview() {
    AppTheme {
        CardWithPopup(
            Modifier,
            title = "Title",
            descriptions = listOf("test"),
            onCardClicked = {},
            popupActions = listOf()
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDark() {
    AppTheme {
        CardWithPopup(
            Modifier,
            title = "Title",
            descriptions = listOf("test"),
            onCardClicked = {},
            popupActions = listOf()
        )
    }
}