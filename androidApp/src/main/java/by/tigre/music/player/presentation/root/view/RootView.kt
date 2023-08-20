package by.tigre.music.player.presentation.root.view

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import by.tigre.music.player.R
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.music.player.core.presentation.catalog.di.PlayerViewProvider
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.presentation.background.BackgroundService
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.music.player.tools.platform.compose.ComposableView
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

class RootView(
    private val component: Root,
    private val catalogViewProvider: CatalogViewProvider,
    private val playerViewProvider: PlayerViewProvider,
    private val currentQueueViewProvider: CurrentQueueViewProvider
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
            DrawMain()
        } else {
            DrawPermissionsRequest(permissionState)
        }
    }

    @Composable
    private fun DrawMain() {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            launch {
                component.onStartServiceEvent
                    .collect {
                        context.startForegroundService(Intent(context, BackgroundService::class.java))
                    }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Column {
                    playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier)

                    NavigationBar {
                        val pages = component.pages.subscribeAsState()

                        NavigationBarItem(
                            modifier = Modifier.navigationBarsPadding(),
                            selected = pages.value.active.instance is Root.PageComponentChild.Queue,
                            onClick = { component.selectPage(0) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_format_list_numbered_24),
                                    contentDescription = "Playlist"
                                )
                            },
                            label = {
                                Text(
                                    text = "Playlist",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            },
                        )

                        NavigationBarItem(
                            modifier = Modifier.navigationBarsPadding(),
                            selected = pages.value.active.instance is Root.PageComponentChild.Catalog,
                            onClick = { component.selectPage(1) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.outline_library_music_24),
                                    contentDescription = "Library"
                                )
                            },
                            label = {
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            },
                        )
                    }
                }
            }
        ) { paddings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings)
            ) {
                Children(stack = component.pages, animation = stackAnimation()) {
                    when (val child = it.instance) {
                        is Root.PageComponentChild.Catalog -> catalogViewProvider.createRootView(child.component).Draw(Modifier)
                        is Root.PageComponentChild.Queue -> currentQueueViewProvider.createCurrentQueueView(child.component).Draw(Modifier)
                    }
                }

            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun DrawPermissionsRequest(permissionState: PermissionState) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
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
}
