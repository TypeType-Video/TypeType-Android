package dev.typetype.android.domain.diagnostics

data class SabrDiagnosticDetail(
    val sessionFingerprint: String,
    val generation: Long?,
    val state: State?,
    val track: Track?,
    val blocker: Blocker?,
    val terminal: Terminal?,
    val recovery: Recovery?,
    val bufferProgress: BufferProgress?,
) {
    fun redactedSummary(): String = buildList {
        add("session=$sessionFingerprint")
        generation?.let { add("generation=$it") }
        state?.let { add("state=${it.wireValue}") }
        track?.let { add("track=${it.wireValue}") }
        blocker?.let { add("blocker=${it.wireValue}") }
        terminal?.let { add("terminal=${it.wireValue}") }
        recovery?.let { add("recovery=${it.wireValue}") }
        bufferProgress?.let { add("buffer=${it.wireValue}") }
    }.joinToString(" ")

    enum class State(val wireValue: String) {
        Idle("idle"),
        Preparing("preparing"),
        Requesting("requesting"),
        Repositioning("repositioning"),
        WaitingForLive("waiting-live"),
        Throttled("throttled"),
        NetworkFailed("network-failed"),
        Terminal("terminal"),
        Stopped("stopped"),
        Ready("ready"),
    }

    enum class Track(val wireValue: String) {
        Audio("audio"),
        Video("video"),
    }

    enum class Blocker(val wireValue: String) {
        SegmentPending("segment-pending"),
        Discontinuity("discontinuity"),
        WindowCapped("window-capped"),
        ProtectedNoMedia("protected-no-media"),
        Token("token"),
        Reload("reload"),
        Policy("policy"),
        Upstream("upstream"),
        Other("other"),
    }

    enum class Terminal(val wireValue: String) {
        SegmentStalled("segment-stalled"),
        ProtectedNoMedia("protected-no-media"),
        Token("token"),
        UmpResponse("ump-response"),
        Upstream("upstream"),
        MissingSession("missing-session"),
        StaleGeneration("stale-generation"),
        ExpiredSession("expired-session"),
        Other("other"),
    }

    enum class Recovery(val wireValue: String) {
        FreshSession("fresh-session"),
        LowerVideoFormat("lower-video-format"),
        Other("other"),
    }

    enum class BufferProgress(val wireValue: String) {
        Empty("empty"),
        Initial("initial"),
        Advanced("advanced"),
        Stalled("stalled"),
        Regressed("regressed"),
    }
}
