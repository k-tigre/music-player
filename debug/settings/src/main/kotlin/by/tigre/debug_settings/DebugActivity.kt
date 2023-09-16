package by.tigre.debug_settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import by.tigre.music.player.logger.DbLogger
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import com.arkivanov.decompose.defaultComponentContext

class DebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = DebugComponent.Impl(
            componentContext = BaseComponentContextImpl(defaultComponentContext()),
            logsProvider = DbLogger.getLogsProvider()
        )

        setContent {
            MaterialTheme {
                DebugView(component).Draw(Modifier)
            }
        }
    }
}
