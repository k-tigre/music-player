package by.tigre.debug_settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import by.tigre.logger.DbLogger
import by.tigre.media.platform.presentation.BaseComponentContext
import by.tigre.media.platform.presentation.BaseComponentContextImpl
import com.arkivanov.decompose.defaultComponentContext

open class DebugActivity : AppCompatActivity() {

    protected open fun createExtraPages(componentContext: BaseComponentContext): List<DebugPageComponent> =
        emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentContext = BaseComponentContextImpl(defaultComponentContext())
        val component = DebugComponent.Impl(
            componentContext = componentContext,
            logsProvider = DbLogger.getLogsProvider(),
            extraPages = createExtraPages(componentContext),
        )

        setContent {
            MaterialTheme {
                DebugView(component).Draw(Modifier)
            }
        }
    }
}
