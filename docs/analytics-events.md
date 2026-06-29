# Analytics events

Auto-generated from `@AnalyticsScope` annotations. Regenerate:

```bash
./gradlew :tools:analytics:generateAnalyticsDocs
```

| Module | Kind | Event name | ID | Player | AudioBook | Desktop | Skip | Description |
|--------|------|------------|-----|:------:|:---------:|:-------:|:----:|-------------|
| book | action | `book_catalog_open_folder_settings` | CatalogOpenFolderSettings |  | ✓ |  |  | Open folder selection for audiobook library |
| book | action | `book_catalog_select_book` | CatalogSelectBook |  | ✓ |  |  | Select book in library |
| book | action | `book_nav_open_catalog` | NavOpenCatalog |  | ✓ |  |  | Open audiobook library catalog |
| book | action | `book_nav_open_night_timer` | NavOpenNightTimer |  | ✓ |  |  | Open night timer settings |
| book | screen | `book_screen_book_list` | BookList |  | ✓ |  |  | Book list in library |
| book | screen | `book_screen_catalog` | Catalog |  | ✓ |  |  | Audiobook library catalog root |
| book | screen | `book_screen_folder_selection` | FolderSelection |  | ✓ |  |  | Folder selection for audiobook sources |
| book | screen | `book_screen_night_timer` | NightTimerSettings |  | ✓ |  |  | Night timer settings |
| book | screen | `book_screen_playback_speed` | PlaybackSpeedSettings |  | ✓ |  |  | Playback speed settings |
| common | action | `common_nav_open_equalizer` | NavOpenEqualizer | ✓ | ✓ | ✓ |  | Open equalizer screen |
| common | action | `common_nav_open_player` | NavOpenPlayer | ✓ | ✓ | ✓ |  | Open full player screen |
| common | action | `common_nav_open_settings` | NavOpenSettings | ✓ |  |  |  | Open settings screen |
| common | action | `common_player_next` | PlayerNext | ✓ | ✓ | ✓ |  | Skip to next track or chapter |
| common | action | `common_player_pause` | PlayerPause | ✓ | ✓ | ✓ |  | Pause playback |
| common | action | `common_player_play` | PlayerPlay | ✓ | ✓ | ✓ |  | Play / resume playback |
| common | action | `common_player_prev` | PlayerPrev | ✓ | ✓ | ✓ |  | Skip to previous track or chapter |
| common | action | `common_player_repeat_cycle` | PlayerRepeatCycle | ✓ |  | ✓ |  | Cycle repeat mode (off / all / one) |
| common | action | `common_player_seek_back_15` | PlayerSeekBack15 | ✓ | ✓ | ✓ |  | Seek backward 15 seconds |
| common | action | `common_player_seek_back_60` | PlayerSeekBack60 | ✓ | ✓ | ✓ |  | Seek backward 60 seconds |
| common | action | `common_player_seek_forward_15` | PlayerSeekForward15 | ✓ | ✓ | ✓ |  | Seek forward 15 seconds |
| common | action | `common_player_seek_forward_60` | PlayerSeekForward60 | ✓ | ✓ | ✓ |  | Seek forward 60 seconds |
| common | action | `common_player_shuffle_toggle` | PlayerShuffleToggle | ✓ |  | ✓ |  | Toggle shuffle mode |
| common | screen | `common_screen_equalizer` | Equalizer | ✓ | ✓ | ✓ |  | Equalizer screen |
| common | screen | `common_screen_player` | Player | ✓ | ✓ | ✓ |  | Full player screen |
| common | screen | `common_screen_root_overlay` | RootOverlay | ✓ |  | ✓ | yes | Root overlay without a dedicated screen (not sent to analytics) |
| common | screen | `common_screen_settings` | Settings | ✓ |  |  |  | Settings screen |
| music | action | `music_catalog_add_album_to_queue` | CatalogAddAlbumToQueue | ✓ |  | ✓ |  | Add entire album to queue from album list |
| music | action | `music_catalog_add_song_to_queue` | CatalogAddSongToQueue | ✓ |  | ✓ |  | Add song to queue from album song list |
| music | action | `music_catalog_play_album` | CatalogPlayAlbum | ✓ |  | ✓ |  | Play entire album from album list |
| music | action | `music_catalog_play_song` | CatalogPlaySong | ✓ |  | ✓ |  | Play song from album song list |
| music | action | `music_catalog_search` | CatalogSearch | ✓ |  | ✓ |  | Catalog search performed after debounce |
| music | action | `music_default_player_prompt_clicked` | DefaultPlayerPromptClicked | ✓ |  |  |  | Default player onboarding prompt action clicked |
| music | action | `music_default_player_prompt_shown` | DefaultPlayerPromptShown | ✓ |  |  |  | Default player onboarding prompt shown |
| music | action | `music_external_audio_opened` | ExternalAudioOpened | ✓ |  |  |  | External audio file opened from another app |
| music | action | `music_external_audio_overlay_ended` | ExternalAudioOverlayEnded | ✓ |  |  |  | External audio overlay ended |
| music | action | `music_favorite_toggle` | FavoriteToggle | ✓ |  | ✓ |  | Toggle track favorite state |
| music | action | `music_nav_open_catalog` | NavOpenCatalog | ✓ |  | ✓ |  | Open music catalog tab |
| music | action | `music_nav_open_favorites` | NavOpenFavorites | ✓ |  | ✓ |  | Open favorites tab |
| music | action | `music_nav_open_playlists` | NavOpenPlaylists | ✓ |  | ✓ |  | Open playlists tab |
| music | action | `music_nav_open_queue` | NavOpenQueue | ✓ |  | ✓ |  | Open playback queue |
| music | action | `music_playlist_add_tracks` | PlaylistAddTracks | ✓ |  | ✓ |  | Add tracks to a playlist |
| music | action | `music_playlist_create` | PlaylistCreate | ✓ |  | ✓ |  | Create a new playlist |
| music | action | `music_playlist_delete` | PlaylistDelete | ✓ |  | ✓ |  | Delete a playlist |
| music | action | `music_playlist_play_all` | PlaylistPlayAll | ✓ |  | ✓ |  | Play all tracks in a playlist |
| music | action | `music_queue_open_album` | QueueOpenAlbum | ✓ |  | ✓ |  | Open album from queue item |
| music | action | `music_queue_open_artist` | QueueOpenArtist | ✓ |  | ✓ |  | Open artist from queue item |
| music | action | `music_queue_song_selected` | QueueSongSelected | ✓ |  | ✓ |  | Select song in current queue |
| music | screen | `music_screen_albums_list` | AlbumsList | ✓ |  | ✓ |  | Albums list for selected artist |
| music | screen | `music_screen_artists_list` | ArtistsList | ✓ |  | ✓ |  | Artists list in music catalog |
| music | screen | `music_screen_catalog_tab` | CatalogTab | ✓ |  | ✓ |  | Music catalog tab |
| music | screen | `music_screen_favorites` | FavoritesTab | ✓ |  | ✓ |  | Favorites tab |
| music | screen | `music_screen_playlist_detail` | PlaylistDetail | ✓ |  | ✓ |  | Playlist detail screen |
| music | screen | `music_screen_playlists_list` | PlaylistsList | ✓ |  | ✓ |  | Playlists list tab |
| music | screen | `music_screen_queue` | Queue | ✓ |  | ✓ |  | Playback queue tab |
| music | screen | `music_screen_songs_list` | SongsList | ✓ |  | ✓ |  | Songs list for selected album |
