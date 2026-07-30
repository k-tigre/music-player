package by.tigre.media.platform.entitlements

enum class Feature {
    Equalizer,
    SleepTimerAdvanced,
    HomeWidget,
    ContinueListeningExpanded,
    UnlimitedPlaylists,
    ;
}

fun Feature.minTier(): Tier = when (this) {
    Feature.Equalizer -> Tier.Plus
    Feature.SleepTimerAdvanced -> Tier.Plus
    Feature.HomeWidget -> Tier.Pro
    Feature.ContinueListeningExpanded -> Tier.Plus
    Feature.UnlimitedPlaylists -> Tier.Pro
}
