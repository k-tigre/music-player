package by.tigre.music.player.core.presentation.catalog.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import by.tigre.music.player.core.presentation.catalog.component.CatalogFolderSelectorComponent
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalDecomposeApi::class)
class CatalogFolderView(
    private val component: CatalogFolderSelectorComponent,
) : ComposableView {

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun Draw() {
        val isHasPermission = component.isPermissionGranted.collectAsState()

        if (isHasPermission.value) {
            DrawRootFolder()
        } else {
            DrawPermissionsRequest()
        }
    }

    @Composable
    private fun DrawPermissionsRequest() {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textToShow = "The access file is important for this app. Please grant the permission."
            Text(textToShow)
            Button(onClick = { component.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }

    @Composable
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
    }
}
