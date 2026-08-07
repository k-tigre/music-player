package by.tigre.media.platform.entitlements

enum class Feature {
    Equalizer,
    EqDeviceProfiles,
    SleepTimerAdvanced,
    HomeWidget,
    ContinueListeningExpanded,
    UnlimitedPlaylists,
    BookSpaces,
    ;
}

fun Feature.minTier(): Tier = when (this) {
    Feature.Equalizer -> Tier.Plus
    Feature.EqDeviceProfiles -> Tier.Pro
    Feature.SleepTimerAdvanced -> Tier.Plus
    Feature.HomeWidget -> Tier.Pro
    Feature.ContinueListeningExpanded -> Tier.Plus
    Feature.UnlimitedPlaylists -> Tier.Pro
    Feature.BookSpaces -> Tier.Pro
}
