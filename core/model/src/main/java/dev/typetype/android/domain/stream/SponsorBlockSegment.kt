package dev.typetype.android.domain.stream

data class SponsorBlockSegment(
    val startMs: Long,
    val endMs: Long,
    val category: SponsorCategory,
    val action: SponsorAction,
)

enum class SponsorCategory(val key: String) {
    Sponsor("sponsor"),
    SelfPromo("selfpromo"),
    ExclusiveAccess("exclusive_access"),
    Interaction("interaction"),
    Poi("poi_highlight"),
    Intro("intro"),
    Outro("outro"),
    Preview("preview"),
    MusicOffTopic("music_offtopic"),
    Filler("filler"),
    Chapter("chapter"),
    Unknown(""),
    ;

    companion object {
        fun fromKey(key: String): SponsorCategory = when (key.lowercase()) {
            "sponsor" -> Sponsor
            "selfpromo" -> SelfPromo
            "exclusive_access" -> ExclusiveAccess
            "interaction" -> Interaction
            "poi_highlight" -> Poi
            "intro" -> Intro
            "outro" -> Outro
            "preview" -> Preview
            "music_offtopic" -> MusicOffTopic
            "filler" -> Filler
            "chapter" -> Chapter
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
