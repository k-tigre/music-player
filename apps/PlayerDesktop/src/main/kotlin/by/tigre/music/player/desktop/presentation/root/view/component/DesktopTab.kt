package by.tigre.music.player.desktop.presentation.root.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.tigre.music.player.desktop.presentation.theme.DesktopGreen
import by.tigre.music.player.desktop.presentation.theme.DesktopPanel
import by.tigre.music.player.desktop.presentation.theme.DesktopSubText
import by.tigre.music.player.desktop.presentation.theme.DesktopTabBg

@Composable
internal fun DesktopTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (selected) DesktopPanel else DesktopTabBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) DesktopGreen else DesktopSubText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
