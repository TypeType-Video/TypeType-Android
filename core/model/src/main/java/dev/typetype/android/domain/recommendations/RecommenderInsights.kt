package dev.typetype.android.domain.recommendations

data class InterestExample(val url: String, val title: String?, val thumbnailUrl: String?)

data class InterestGroup(
    val id: String,
    val label: String,
    val weight: Double,
    val basedOn: Int,
    val pinned: Boolean,
    val muted: Boolean,
    val examples: List<InterestExample> = emptyList(),
) {
    val custom: Boolean get() = id.startsWith("custom:")
    val pinnable: Boolean get() = id.startsWith("group:")
}

data class ProfileChannel(
    val id: String,
    val name: String,
    val weight: Double,
    val basedOn: Int,
    val avatarUrl: String?,
)

data class SessionBoost(val ref: String, val label: String, val expiresAt: Long)

data class RecommenderProfile(
    val groups: List<InterestGroup> = emptyList(),
    val channels: List<ProfileChannel> = emptyList(),
    val sessionBoosts: List<SessionBoost> = emptyList(),
)

data class DataItem(
    val id: String,
    val title: String,
    val channel: String?,
    val at: Long,
    val watchedPercent: Int?,
    val excluded: Boolean,
    val thumbnailUrl: String?,
    val viewings: Int,
)

data class DataCategory(
    val id: String,
    val count: Int,
    val used: Boolean,
    val retentionDays: Int?,
    val collect: Boolean?,
    val items: List<DataItem> = emptyList(),
)

data class TraceSource(val kind: String, val received: Int)

data class TraceRow(val id: String, val title: String, val why: String, val videos: Int)

data class TraceScore(
    val url: String,
    val title: String,
    val channel: String,
    val row: String,
    val total: Double,
    val factors: Map<String, Double>,
)

data class PageTrace(
    val algorithm: String,
    val builtAt: Long,
    val sources: List<TraceSource>,
    val candidates: Int,
    val dropped: Map<String, Int>,
    val shown: Int,
    val rows: List<TraceRow>,
    val scores: List<TraceScore>,
)

data class FeedbackEntry(
    val id: String,
    val kind: String,
    val verdict: String,
    val comment: String?,
    val reason: String?,
    val title: String?,
    val channelName: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    val algorithmId: String,
    val createdAt: Long,
)
