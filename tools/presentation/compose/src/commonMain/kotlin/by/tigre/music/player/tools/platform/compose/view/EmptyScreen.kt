package by.tigre.music.player.tools.platform.compose.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `by`.tigre.music.player.tools.platform.compose.resources.Res
import `by`.tigre.music.player.tools.platform.compose.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String,
    actionTitle: String? = null,
    reloadAction: () -> Unit
) {
    val resolvedTitle = title ?: stringResource(Res.string.screen_state_empty_title)
    val resolvedActionTitle = actionTitle ?: stringResource(Res.string.reload_action)
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = stringResource(Res.string.cd_error),
            modifier = Modifier.size(64.dp),
        )

        Text(
            text = resolvedTitle,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = message,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            onClick = reloadAction,
        ) {
            Text(
                text = resolvedActionTitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
