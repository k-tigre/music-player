package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.presentation.audiobook_catalog.component.SettingsHubComponent
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.settings_about
import by.tigre.audiobook.core.presentation.catalog.resources.settings_folders
import by.tigre.audiobook.core.presentation.catalog.resources.settings_hint_add_books
import by.tigre.audiobook.core.presentation.catalog.resources.settings_theme
import by.tigre.audiobook.core.presentation.catalog.resources.settings_title
import by.tigre.media.platform.tools.platform.compose.ComposableView
import org.jetbrains.compose.resources.stringResource

class SettingsHubView(
    private val component: SettingsHubComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_title)) },
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
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.settings_hint_add_books),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_theme)) },
                    leadingContent = {
                        Icon(Icons.Filled.Palette, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = component::onThemeClick),
                )
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_folders)) },
                    leadingContent = {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = component::onFoldersClick),
                )
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_about)) },
                    leadingContent = {
                        Icon(Icons.Filled.Info, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = component::onAboutClick),
                )
            }
        }
    }
}
