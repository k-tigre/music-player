package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `by`.tigre.media.platform.tools.platform.compose.resources.Res
import `by`.tigre.media.platform.tools.platform.compose.resources.*
import org.jetbrains.compose.resources.stringResource

private val CoverStartPadding = 12.dp
private val RowEndPadding = 4.dp
private val CoverTextGap = 12.dp
private val RowVerticalPadding = 8.dp
private val MenuButtonSize = 48.dp
private val CoverSize = 52.dp

@Composable
fun CardWithPopup(
    modifier: Modifier,
    title: String,
    popupActions: List<PopupAction>? = null,
    cardClickPopupActions: List<PopupAction>? = null,
    onCardClicked: () -> Unit,
    descriptions: List<String>,
    leadingContent: (@Composable () -> Unit)? = null,
    surface: ListRowSurface = ListRowSurface.Plain,
    showDivider: Boolean = true,
    containerColor: Color? = null,
) {
    var popupControl by remember { mutableStateOf(false) }
    var activePopupActions by remember { mutableStateOf<List<PopupAction>?>(null) }

    val cardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        disabledContentColor = MaterialTheme.colorScheme.onSurface,
    )

    val onRowClick = {
        if (cardClickPopupActions != null) {
            activePopupActions = cardClickPopupActions
            popupControl = true
        } else {
            onCardClicked()
        }
    }

    val onMenuClick = {
        activePopupActions = popupActions
        popupControl = true
    }

    when (surface) {
        ListRowSurface.CardInset, ListRowSurface.CardFullWidth -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                onClick = onRowClick,
                colors = cardColors,
            ) {
                ListRowContent(
                    title = title,
                    descriptions = descriptions,
                    leadingContent = leadingContent,
                    popupActions = popupActions,
                    popupExpanded = popupControl,
                    activePopupActions = activePopupActions,
                    onDismissPopup = { popupControl = false },
                    onMenuClick = onMenuClick,
                )
            }
        }

        ListRowSurface.Plain -> {
            Column(modifier = modifier.fillMaxWidth()) {
                ListRowContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (containerColor != null) {
                                Modifier.background(containerColor)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(onClick = onRowClick),
                    title = title,
                    descriptions = descriptions,
                    leadingContent = leadingContent,
                    popupActions = popupActions,
                    popupExpanded = popupControl,
                    activePopupActions = activePopupActions,
                    onDismissPopup = { popupControl = false },
                    onMenuClick = onMenuClick,
                )
                if (showDivider) {
                    ListRowDivider(
                        modifier = Modifier.padding(start = plainDividerInset(leadingContent != null)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListRowContent(
    title: String,
    descriptions: List<String>,
    leadingContent: (@Composable () -> Unit)?,
    popupActions: List<PopupAction>?,
    popupExpanded: Boolean,
    activePopupActions: List<PopupAction>?,
    onDismissPopup: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = CoverStartPadding,
                end = RowEndPadding,
                top = RowVerticalPadding,
                bottom = RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(CoverTextGap))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            descriptions.forEach { item ->
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (popupActions != null) {
            Box {
                IconButton(
                    modifier = Modifier
                        .defaultMinSize(minWidth = MenuButtonSize, minHeight = MenuButtonSize)
                        .size(MenuButtonSize),
                    onClick = onMenuClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.cd_more_options),
                    )
                }
                DropdownMenu(
                    expanded = popupExpanded,
                    onDismissRequest = onDismissPopup,
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
                                onDismissPopup()
                                action()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun plainDividerInset(hasCover: Boolean): Dp =
    if (hasCover) CoverStartPadding + CoverSize + CoverTextGap else CoverStartPadding

@Composable
fun ListRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

data class PopupAction(val title: String, val action: () -> Unit)
