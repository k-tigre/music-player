package by.tigre.music.player.presentation.settings.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
            ThemeSettingsContent(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                themeMode = themeSettings.mode,
                onThemeModeChange = component::setThemeMode,
                dynamicColorEnabled = themeSettings.dynamicColor,
                onDynamicColorChange = component::setDynamicColor,
                dynamicColorAvailable = isDynamicColorSupported(),
                contrast = themeSettings.contrast,
                onContrastChange = component::setContrast,
            )
        }
    }
}
