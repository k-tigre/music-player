package by.tigre.music.player

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import by.tigre.music.player.core.presentation.catalog.di.CatalogComponentProvider
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.presentation.base.BaseComponentContextImpl
import by.tigre.music.player.tools.platform.compose.AppMaterial
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        findViewById<View>(R.id.play).setOnClickListener {
//            this.startService(Intent(this, BackgroundService::class.java))
//        }
//
//        findViewById<View>(R.id.stop).setOnClickListener {
//            this.stopService(Intent(this, BackgroundService::class.java))
//        }

        val component = CatalogComponentProvider.Impl().createRootCatalogComponent(
            context = BaseComponentContextImpl(defaultComponentContext()),
        )

        setContent {
            AppMaterial.AppTheme {
                CatalogViewProvider.Impl().createRootView(component).Draw()
            }
        }
    }
}
