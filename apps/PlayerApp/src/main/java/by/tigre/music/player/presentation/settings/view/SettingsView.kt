package by.tigre.music.player.presentation.settings.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.isDynamicColorSupported
import by.tigre.media.platform.tools.platform.compose.view.ThemeSettingsContent
import by.tigre.music.player.R
import by.tigre.music.player.presentation.settings.component.SettingsComponent

class SettingsView(
    private val component: SettingsComponent,
) : ComposableView {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val themeSettings by component.themeSettings.collectAsState()
        val tipsCount by component.tipsCount.collectAsState()
        val title = stringResource(R.string.settings_title)

        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = component::close) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_upgrade)) },
                    supportingContent = { Text(stringResource(R.string.settings_upgrade_summary)) },
                    trailingContent = {
                        TextButton(onClick = component::upgrade) {
                            Text(stringResource(R.string.settings_upgrade))
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_restore)) },
                    trailingContent = {
                        TextButton(onClick = component::restorePurchases) {
                            Text(stringResource(R.string.settings_restore))
                        }
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_tips)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_tips_count, tipsCount))
                    },
                    trailingContent = {
                        TextButton(onClick = component::tips) {
                            Text(stringResource(R.string.settings_tips))
                        }
                    },
                )
                if (tipsCount > 0) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_supporter_badge)) },
                    )
                }
                ThemeSettingsContent(
                    themeMode = themeSettings.mode,
                    onThemeModeChange = component::setThemeMode,
                    dynamicColorEnabled = themeSettings.dynamicColor,
                    onDynamicColorChange = component::setDynamicColor,
                    dynamicColorAvailable = isDynamicColorSupported(),
                    contrast = themeSettings.contrast,
                    onContrastChange = component::setContrast,
                )
                Text(
                    text = stringResource(R.string.settings_version, component.appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clickable(onClick = component::onVersionClick),
                )
            }
        }
    }
}
