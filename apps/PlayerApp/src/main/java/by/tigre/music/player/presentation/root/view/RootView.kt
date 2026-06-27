package by.tigre.music.player.presentation.root.view

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.ui.unit.dp
import by.tigre.music.player.R
import by.tigre.music.player.core.data.playlist.AddToPlaylistCoordinator
import by.tigre.music.player.core.data.playlist.PlaylistRepository
import by.tigre.music.player.core.presentation.catalog.di.CatalogViewProvider
import by.tigre.media.platform.player.di.PlayerViewProvider
import by.tigre.media.platform.player.view.PlayerView
import by.tigre.music.player.core.presentation.playlist.current.di.CurrentQueueViewProvider
import by.tigre.music.player.core.presentation.playlist.library.di.PlaylistsViewProvider
import by.tigre.music.player.core.presentation.favorites.di.FavoritesViewProvider
import by.tigre.music.player.core.presentation.favorites.view.MusicPlayerFavoriteTopBar
import by.tigre.music.player.core.data.playback.PlaybackController
import by.tigre.music.player.core.data.favorites.FavoritesRepository
import by.tigre.music.player.core.presentation.playlist.library.view.AddToPlaylistBottomSheet
import by.tigre.music.player.platform.DefaultMusicPlayerRole
import by.tigre.music.player.presentation.root.component.Root
import by.tigre.media.platform.tools.analytics.music.MusicEventAnalytics
import by.tigre.media.platform.tools.analytics.music.MusicEvents
import by.tigre.music.player.presentation.settings.view.SettingsView
import by.tigre.media.platform.tools.platform.compose.ComposableView
import by.tigre.media.platform.tools.platform.compose.view.BottomBarContainer
import by.tigre.media.platform.tools.platform.compose.view.BottomBarNavigationBarInsets
import by.tigre.media.platform.tools.platform.compose.view.LocalBottomBarHeight
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

class RootView(
    private val component: Root,
    private val catalogViewProvider: CatalogViewProvider,
    private val playerViewProvider: PlayerViewProvider,
    private val currentQueueViewProvider: CurrentQueueViewProvider,
    private val playlistsViewProvider: PlaylistsViewProvider,
    private val favoritesViewProvider: FavoritesViewProvider,
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackController: PlaybackController,
    private val addToPlaylistCoordinator: AddToPlaylistCoordinator,
    private val eventAnalytics: MusicEventAnalytics,
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
            DrawDefaultPlayerPrompt()
        } else {
            DrawPermissionsRequest(permissionState)
        }
    }

    @Composable
    private fun DrawMain() {
        val addToPlaylistRequest by addToPlaylistCoordinator.request.collectAsState()
        val playlists by playlistRepository.allPlaylists.collectAsState(initial = emptyList())
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val addToPlaylistAddedMessage = stringResource(R.string.add_to_playlist_added_snackbar)
        val playlistNameTakenMessage = stringResource(R.string.playlist_name_taken)

        Children(
            stack = component.mainComponent,
            animation = stackAnimation(animator = fade())
        ) {
            when (val child = it.instance) {
                is Root.MainComponentChild.Main -> DrawPages()
                is Root.MainComponentChild.Player -> {
                    val playerConfig = PlayerView.Config(
                        emptyScreenAction = {},
                        emptyScreenTitle = stringResource(R.string.player_queue_empty_title),
                        emptyScreenMessage = stringResource(R.string.player_queue_empty_message),
                        emptyScreenActionTitle = stringResource(R.string.player_queue_empty_action),
                        equalizerMenuLabel = stringResource(R.string.player_equalizer_menu),
                        queueMenuLabel = stringResource(R.string.player_queue_menu),
                        returnToQueueLabel = stringResource(R.string.cd_return_to_queue),
                        settingsMenuLabel = stringResource(R.string.player_settings_menu),
                    )
                    playerViewProvider.createPlayerView(
                        component = child.component,
                        config = playerConfig,
                        topBarContent = {
                            MusicPlayerFavoriteTopBar(
                                component = child.component,
                                config = playerConfig,
                                playbackController = playbackController,
                                favoritesRepository = favoritesRepository,
                                eventAnalytics = eventAnalytics,
                            )
                        },
                    ).Draw(Modifier.fillMaxSize())
                }

                is Root.MainComponentChild.Equalizer ->
                    playerViewProvider.createEqualizerView(child.component).Draw(Modifier.fillMaxSize())

                is Root.MainComponentChild.Settings ->
                    SettingsView(child.component).Draw(Modifier.fillMaxSize())
            }
        }

        SnackbarHost(hostState = snackbarHostState)

        AddToPlaylistBottomSheet(
            request = addToPlaylistRequest,
            playlists = playlists,
            onDismiss = addToPlaylistCoordinator::dismiss,
            onSelectPlaylist = { playlistId ->
                val request = addToPlaylistRequest ?: return@AddToPlaylistBottomSheet
                scope.launch {
                    playlistRepository.addSongs(playlistId, request.songIds)
                    eventAnalytics.trackEvent(MusicEvents.Action.PlaylistAddTracks(request.songIds.size))
                    addToPlaylistCoordinator.dismiss()
                    snackbarHostState.showSnackbar(addToPlaylistAddedMessage)
                }
            },
            onCreateAndAdd = { playlistName ->
                val request = addToPlaylistRequest ?: return@AddToPlaylistBottomSheet
                scope.launch {
                    val trimmedName = playlistName.trim()
                    if (trimmedName.isEmpty()) return@launch
                    if (playlistRepository.isNameTaken(trimmedName)) {
                        snackbarHostState.showSnackbar(playlistNameTakenMessage)
                        return@launch
                    }
                    val playlistId = playlistRepository.createPlaylist(trimmedName)
                    playlistRepository.addSongs(playlistId, request.songIds)
                    eventAnalytics.trackEvent(MusicEvents.Action.PlaylistAddTracks(request.songIds.size))
                    addToPlaylistCoordinator.dismiss()
                    snackbarHostState.showSnackbar(addToPlaylistAddedMessage)
                    component.openPlaylistDetail(playlistId)
                }
            },
        )
    }

    @Composable
    private fun DrawPages() {
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeight) {
                Children(
                    stack = component.pages,
                    animation = stackAnimation(animator = scale(frontFactor = 0.8f, backFactor = 0.8f) + fade())
                ) {
                    when (val child = it.instance) {
                        is Root.PageComponentChild.Catalog -> catalogViewProvider.createRootView(child.component)
                        is Root.PageComponentChild.Playlists -> playlistsViewProvider.createRootView(child.component)
                        is Root.PageComponentChild.Favorites -> favoritesViewProvider.createFavoritesView(child.component)
                        is Root.PageComponentChild.Queue -> currentQueueViewProvider.createCurrentQueueView(child.component)
                    }.Draw(Modifier.fillMaxSize())
                }
            }

            BottomBarContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        bottomBarHeight = with(density) { size.height.toDp() }
                    },
            ) {
                playerViewProvider.createSmallPlayerView(component.playerComponent).Draw(Modifier)

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp,
                    windowInsets = BottomBarNavigationBarInsets,
                ) {
                    val pages = component.pages.subscribeAsState()

                    NavigationBarItem(
                        selected = pages.value.active.instance is Root.PageComponentChild.Queue,
                        onClick = { component.selectPage(0) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.QueueMusic,
                                contentDescription = stringResource(R.string.cd_nav_playlist)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_playlist),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )

                    NavigationBarItem(
                        selected = pages.value.active.instance is Root.PageComponentChild.Playlists,
                        onClick = { component.selectPage(1) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.PlaylistPlay,
                                contentDescription = stringResource(R.string.cd_nav_playlists)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_playlists),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )

                    NavigationBarItem(
                        selected = pages.value.active.instance is Root.PageComponentChild.Favorites,
                        onClick = { component.selectPage(2) },
                        icon = {
                            Icon(
                                imageVector = if (pages.value.active.instance is Root.PageComponentChild.Favorites) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.cd_nav_favorites)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_favorites),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )

                    NavigationBarItem(
                        selected = pages.value.active.instance is Root.PageComponentChild.Catalog,
                        onClick = { component.selectPage(3) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.LibraryMusic,
                                contentDescription = stringResource(R.string.cd_nav_library)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_library),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun DrawDefaultPlayerPrompt() {
        val showPrompt by component.showDefaultPlayerPrompt.collectAsState()
        val context = LocalContext.current
        val canRequestRole = remember(context) { DefaultMusicPlayerRole.canRequestRole(context) }

        if (showPrompt) {
            AlertDialog(
                onDismissRequest = component::dismissDefaultPlayerPrompt,
                title = { Text(stringResource(R.string.default_player_title)) },
                text = {
                    Text(
                        stringResource(
                            if (canRequestRole) {
                                R.string.default_player_message_role
                            } else {
                                R.string.default_player_message_manual
                            },
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            component.confirmDefaultPlayerPrompt()
                            val intent = when {
                                canRequestRole -> DefaultMusicPlayerRole.createRequestIntent(context)
                                else -> DefaultMusicPlayerRole.createOpenDownloadsIntent(context)
                            } ?: DefaultMusicPlayerRole.createAppDetailsIntent(context)
                            context.startActivity(intent)
                        },
                    ) {
                        Text(
                            stringResource(
                                if (canRequestRole) {
                                    R.string.default_player_action_role
                                } else {
                                    R.string.default_player_action_manual
                                },
                            ),
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = component::dismissDefaultPlayerPrompt) {
                        Text(stringResource(R.string.button_got_it))
                    }
                },
            )
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
                text = stringResource(R.string.permission_audio_rationale)
            )

            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.permission_request))
            }
        }
    }
}
