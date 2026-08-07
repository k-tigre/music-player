package by.tigre.media.platform.entitlements

enum class FeatureMode {
    On,
    Off,
    Paid,
    ;

    companion object {
        fun parse(raw: String?): FeatureMode =
            when (raw?.trim()?.lowercase()) {
                "off" -> Off
                "paid" -> Paid
                else -> On
            }
    }
}
