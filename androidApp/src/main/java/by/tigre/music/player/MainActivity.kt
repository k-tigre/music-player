package by.tigre.music.player

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import by.tigre.music.player.presentation.root.view.RootView
import by.tigre.music.player.tools.platform.compose.AppMaterial
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val component = CatalogComponentProvider.Impl(
            dependency = (application as App).graph
        ).createRootCatalogComponent(
            context = BaseComponentContextImpl(defaultComponentContext()),
        )

        setContent {
            AppMaterial.AppTheme() {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    RootView {
                        CatalogViewProvider.Impl().createRootView(component).Draw()
                    }.Draw()
                }
            }
        }
    }
}
