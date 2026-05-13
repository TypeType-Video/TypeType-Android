package dev.typetype.android.domain.stream

data class SponsorBlockSegment(
    val startMs: Long,
    val endMs: Long,
    val category: SponsorCategory,
    val action: SponsorAction,
)

enum class SponsorCategory {
    Sponsor,
    SelfPromo,
    Interaction,
    Poi,
    Intro,
    Outro,
    Preview,
    MusicOffTopic,
    Filler,
    Unknown;

    companion object {
        fun fromKey(key: String): SponsorCategory = when (key.lowercase()) {
            "sponsor" -> Sponsor
            "selfpromo" -> SelfPromo
            "interaction" -> Interaction
            "poi_highlight" -> Poi
            "intro" -> Intro
            "outro" -> Outro
            "preview" -> Preview
            "music_offtopic" -> MusicOffTopic
            "filler" -> Filler
            else -> Unknown
        }
    }
}

enum class SponsorAction {
    Skip,
    Mute,
    Poi,
    Full,
    Unknown;

    companion object {
        fun fromKey(key: String): SponsorAction = when (key.lowercase()) {
            "skip" -> Skip
            "mute" -> Mute
            "poi" -> Poi
            "full" -> Full
            else -> Unknown
        }
    }
}
