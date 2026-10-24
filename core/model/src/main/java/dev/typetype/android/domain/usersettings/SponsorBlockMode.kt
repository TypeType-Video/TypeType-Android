package dev.typetype.android.domain.usersettings

enum class SponsorBlockMode(val wireValue: String) {
    AutoSkip("auto_skip"),
    MarkOnly("mark_only"),
    Disabled("disabled"),
    ;

    companion object {
        fun fromWireValue(value: String): SponsorBlockMode =
            entries.firstOrNull { it.wireValue == value } ?: MarkOnly
    }
}

val DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS: Map<String, SponsorBlockMode> = mapOf(
    "sponsor" to SponsorBlockMode.AutoSkip,
    "selfpromo" to SponsorBlockMode.AutoSkip,
    "exclusive_access" to SponsorBlockMode.MarkOnly,
    "interaction" to SponsorBlockMode.AutoSkip,
    "poi_highlight" to SponsorBlockMode.MarkOnly,
    "intro" to SponsorBlockMode.AutoSkip,
    "outro" to SponsorBlockMode.AutoSkip,
    "preview" to SponsorBlockMode.AutoSkip,
    "filler" to SponsorBlockMode.AutoSkip,
    "chapter" to SponsorBlockMode.MarkOnly,
    "music_offtopic" to SponsorBlockMode.AutoSkip,
)
