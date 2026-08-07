package by.tigre.audiobook.core.di

import by.tigre.media.platform.entitlements.Feature

data class PaywallRequest(
    val feature: Feature,
    val source: String = feature.name,
    val initialSection: PaywallSection = PaywallSection.Plans,
)

enum class PaywallSection {
    Plans,
    Tips,
}
