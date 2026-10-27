package dev.typetype.android.core.openmoji

data class OpenMojiEntry(val code: String, val label: String)

val OPENMOJI_CATALOG: List<OpenMojiEntry> = listOf(
    OpenMojiEntry("1F60A", "smile"),
    OpenMojiEntry("1F604", "grin"),
    OpenMojiEntry("1F606", "laugh"),
    OpenMojiEntry("1F609", "wink"),
    OpenMojiEntry("1F60D", "love eyes"),
    OpenMojiEntry("1F618", "kiss"),
    OpenMojiEntry("1F917", "hug"),
    OpenMojiEntry("1F929", "star struck"),
    OpenMojiEntry("1F970", "hearts"),
    OpenMojiEntry("1F973", "party"),
    OpenMojiEntry("1F60E", "cool"),
    OpenMojiEntry("1F60F", "smirk"),
    OpenMojiEntry("1F913", "nerd"),
    OpenMojiEntry("1F920", "cowboy"),
    OpenMojiEntry("1F978", "disguise"),
    OpenMojiEntry("1F92A", "crazy"),
    OpenMojiEntry("1F92D", "oops"),
    OpenMojiEntry("1F92B", "shush"),
    OpenMojiEntry("1F914", "thinking"),
    OpenMojiEntry("1F9D0", "monocle"),
    OpenMojiEntry("1F60C", "relaxed"),
    OpenMojiEntry("1F634", "sleep"),
    OpenMojiEntry("1F924", "drool"),
    OpenMojiEntry("1F60B", "yum"),
    OpenMojiEntry("1F61C", "tongue"),
    OpenMojiEntry("1F911", "money"),
    OpenMojiEntry("1F4AA", "muscle"),
    OpenMojiEntry("1F47B", "ghost"),
    OpenMojiEntry("1F47D", "alien"),
    OpenMojiEntry("1F916", "robot"),
    OpenMojiEntry("1F47E", "monster"),
    OpenMojiEntry("1F479", "ogre"),
    OpenMojiEntry("1F47A", "goblin"),
    OpenMojiEntry("1F480", "skull"),
    OpenMojiEntry("1F383", "pumpkin"),
    OpenMojiEntry("1F984", "unicorn"),
    OpenMojiEntry("1F409", "dragon"),
    OpenMojiEntry("1F431", "cat"),
    OpenMojiEntry("1F436", "dog"),
    OpenMojiEntry("1F43B", "bear"),
    OpenMojiEntry("1F43C", "panda"),
    OpenMojiEntry("1F428", "koala"),
    OpenMojiEntry("1F981", "lion"),
    OpenMojiEntry("1F98A", "fox"),
    OpenMojiEntry("1F427", "penguin"),
    OpenMojiEntry("1F985", "eagle"),
    OpenMojiEntry("1F989", "owl"),
    OpenMojiEntry("1F99D", "raccoon"),
    OpenMojiEntry("1F43A", "wolf"),
    OpenMojiEntry("1F680", "rocket"),
    OpenMojiEntry("1F525", "fire"),
    OpenMojiEntry("2B50", "star"),
    OpenMojiEntry("1F31F", "glow"),
    OpenMojiEntry("1F308", "rainbow"),
    OpenMojiEntry("26A1", "lightning"),
    OpenMojiEntry("1F3AE", "gamepad"),
    OpenMojiEntry("1F3B8", "guitar"),
    OpenMojiEntry("1F3A7", "headphones"),
    OpenMojiEntry("1F4BB", "laptop"),
    OpenMojiEntry("2615", "coffee"),
    OpenMojiEntry("1F37B", "beer"),
)

fun openMojiUrl(serverBaseUrl: String?, code: String): String? {
    if (serverBaseUrl.isNullOrBlank() || code.isBlank()) return null
    val normalized = serverBaseUrl.trimEnd('/')
    return "$normalized/avatar/openmoji/$code.svg"
}

fun pickOpenMojiCode(seed: String): String {
    if (OPENMOJI_CATALOG.isEmpty()) return ""
    var hash = 0L
    for (c in seed) {
        hash = (hash * 31 + c.code.toLong()) and 0xFFFFFFFFL
    }
    return OPENMOJI_CATALOG[(hash % OPENMOJI_CATALOG.size).toInt()].code
}
