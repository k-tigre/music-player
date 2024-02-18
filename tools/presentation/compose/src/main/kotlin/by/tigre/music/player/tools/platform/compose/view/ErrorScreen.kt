package by.tigre.music.player.tools.platform.compose.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import by.tigre.music.player.tools.platform.compose.AppTheme
import by.tigre.music.playercompose.R

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.screen_state_error_something_went_wrong_title),
    message: String? = null,
    retryAction: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            painter = painterResource(R.drawable.ic_baseline_error_outline_24),
            contentDescription = "error",
            modifier = Modifier.size(64.dp),
        )

        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        if (message != null) {
            Text(
                text = message,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            onClick = retryAction,
        ) {
            Text(
                text = stringResource(R.string.retry_action),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    AppTheme {
        ErrorScreen(
            title = "Что-то пошло очень не так",
            message = "Попробуйте повторить",
            retryAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview2() {
    AppTheme {
        ErrorScreen(
            retryAction = {}
        )
    }
}
