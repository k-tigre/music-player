package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.AboutComponent
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.about_app_name
import by.tigre.audiobook.core.presentation.catalog.resources.about_rate_on_play
import by.tigre.audiobook.core.presentation.catalog.resources.about_version
import by.tigre.audiobook.core.presentation.catalog.resources.settings_about
import by.tigre.media.platform.tools.platform.compose.ComposableView
import org.jetbrains.compose.resources.stringResource

class AboutView(
    private val component: AboutComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val showRateApp by component.showRateApp.collectAsState()

        LaunchedEffect(Unit) {
            component.onScreenShown()
        }

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_about)) },
                    navigationIcon = {
                        IconButton(onClick = component::onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.about_app_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(Res.string.about_version, component.appVersionName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (showRateApp) {
                    Button(
                        onClick = component::onRateAppClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        Text(stringResource(Res.string.about_rate_on_play))
                    }
                }
            }
        }
    }
}
