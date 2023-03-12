package by.tigre.music.player.presentation.root.view

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class RootView(
    private val component: Root,
    private val catalogViewProvider: CatalogViewProvider,
    private val playerViewProvider: PlayerViewProvider
) : ComposableView {

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun Draw(modifier: Modifier) {
        val permissionState = rememberPermissionState(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )

        if (permissionState.status.isGranted) {
            DrawRoot()
        } else {
            DrawPermissionsRequest(permissionState)
        }
    }

    @Composable
    private fun DrawRoot() {
        Column(modifier = Modifier.fillMaxSize()) {
            catalogViewProvider.createRootView(component.catalogComponent).Draw(
                Modifier
                    .weight(1f)
            )

//            playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier.align(Alignment.BottomCenter))
            playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier)
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun DrawPermissionsRequest(permissionState: PermissionState) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = "The access file is important for this app. Please grant the permission."
            )

            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }

    /* @Composable
     private fun DrawRootFolder() {
         val result = remember { mutableStateOf<Uri?>(null) }
         val launcher = rememberLauncherForActivityResult(PickFolder()) {
             result.value = it
         }

         result.value?.let { uri ->
             component.selectNewRootFolder(uri)
         }

         Column(
             modifier = Modifier
                 .fillMaxSize(),
             verticalArrangement = Arrangement.Center,
             horizontalAlignment = Alignment.CenterHorizontally
         ) {
             val textToShow = "Select Catalog Folder"
             Text(textToShow)
             Button(onClick = { launcher.launch(null) }) {
                 Text("select")
             }
         }
     }

     class PickFolder : ActivityResultContract<Unit?, Uri?>() {
         override fun createIntent(context: Context, input: Unit?): Intent {
             return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                 .addCategory(Intent.CATEGORY_DEFAULT)
         }

         override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
             val uri = intent?.data
             val type = intent?.type

             println("parseResult - $uri -- $type")

             return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
         }
     }*/
}
