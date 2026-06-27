package by.tigre.media.platform.tools.platform.compose.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.media.platform.tools.platform.compose.ContrastPreference
import by.tigre.media.platform.tools.platform.compose.ThemeMode
import `by`.tigre.media.platform.tools.platform.compose.resources.Res
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_contrast_default
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_contrast_high
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_contrast_medium
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_contrast_title
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_dynamic_color
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_theme_dark
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_theme_light
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_theme_mode_title
import `by`.tigre.media.platform.tools.platform.compose.resources.settings_theme_system
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
    contrast: ContrastPreference,
    onContrastChange: (ContrastPreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsSection(title = stringResource(Res.string.settings_theme_mode_title)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeModeChip(
                    selected = themeMode == ThemeMode.System,
                    label = stringResource(Res.string.settings_theme_system),
                    onClick = { onThemeModeChange(ThemeMode.System) },
                )
                ThemeModeChip(
                    selected = themeMode == ThemeMode.Light,
                    label = stringResource(Res.string.settings_theme_light),
                    onClick = { onThemeModeChange(ThemeMode.Light) },
                )
                ThemeModeChip(
                    selected = themeMode == ThemeMode.Dark,
                    label = stringResource(Res.string.settings_theme_dark),
                    onClick = { onThemeModeChange(ThemeMode.Dark) },
                )
            }
        }

        if (dynamicColorAvailable) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_dynamic_color),
                checked = dynamicColorEnabled,
                onCheckedChange = onDynamicColorChange,
            )
        }

        SettingsSection(
            title = stringResource(Res.string.settings_contrast_title),
            enabled = !dynamicColorEnabled || !dynamicColorAvailable,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContrastChip(
                    selected = contrast == ContrastPreference.Default,
                    label = stringResource(Res.string.settings_contrast_default),
                    enabled = !dynamicColorEnabled || !dynamicColorAvailable,
                    onClick = { onContrastChange(ContrastPreference.Default) },
                )
                ContrastChip(
                    selected = contrast == ContrastPreference.Medium,
                    label = stringResource(Res.string.settings_contrast_medium),
                    enabled = !dynamicColorEnabled || !dynamicColorAvailable,
                    onClick = { onContrastChange(ContrastPreference.Medium) },
                )
                ContrastChip(
                    selected = contrast == ContrastPreference.High,
                    label = stringResource(Res.string.settings_contrast_high),
                    enabled = !dynamicColorEnabled || !dynamicColorAvailable,
                    onClick = { onContrastChange(ContrastPreference.High) },
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ThemeModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun ContrastChip(
    selected: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
    )
}
