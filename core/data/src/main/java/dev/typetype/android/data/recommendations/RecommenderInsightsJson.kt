package dev.typetype.android.data.recommendations

import dev.typetype.android.domain.recommendations.DataCategory
import dev.typetype.android.domain.recommendations.DataItem
import dev.typetype.android.domain.recommendations.FeedbackEntry
import dev.typetype.android.domain.recommendations.InterestExample
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.PageTrace
import dev.typetype.android.domain.recommendations.ProfileChannel
import dev.typetype.android.domain.recommendations.RecommenderProfile
import dev.typetype.android.domain.recommendations.SessionBoost
import dev.typetype.android.domain.recommendations.TraceRow
import dev.typetype.android.domain.recommendations.TraceScore
import dev.typetype.android.domain.recommendations.TraceSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.toProfile(): RecommenderProfile = RecommenderProfile(
    groups = objects("groups").mapNotNull { it.toInterestGroup() },
    channels = objects("channels").mapNotNull { channel ->
        ProfileChannel(
            id = channel.string("id") ?: return@mapNotNull null,
            name = channel.string("name").orEmpty(),
            weight = channel.double("weight") ?: 0.0,
            basedOn = channel.array("from").size,
            avatarUrl = channel.string("avatarUrl"),
        )
    },
    sessionBoosts = objects("session_boosts").mapNotNull { boost ->
        SessionBoost(
            ref = boost.string("ref") ?: return@mapNotNull null,
            label = boost.string("label").orEmpty(),
            expiresAt = boost.long("expires_at") ?: 0L,
        )
    },
)

private fun JsonObject.toInterestGroup(): InterestGroup? = InterestGroup(
    id = string("id") ?: return null,
    label = string("label").orEmpty(),
    weight = double("weight") ?: 0.0,
    basedOn = array("from").size,
    pinned = bool("pinned") == true,
    muted = bool("muted") == true,
    examples = objects("examples").mapNotNull { example ->
        InterestExample(
            url = example.string("url") ?: return@mapNotNull null,
            title = example.string("title"),
            thumbnailUrl = example.string("thumbnailUrl"),
        )
    },
)

internal fun JsonArray.toDataCategories(): List<DataCategory> = filterIsInstance<JsonObject>().mapNotNull { category ->
    DataCategory(
        id = category.string("id") ?: return@mapNotNull null,
        count = category.int("count") ?: 0,
        used = category.bool("use") != false,
        retentionDays = category.int("retentionDays"),
        collect = category.bool("collect"),
        items = category.objects("items").mapNotNull { it.toDataItem() },
    )
}

private fun JsonObject.toDataItem(): DataItem? = DataItem(
    id = string("id") ?: return null,
    title = string("title").orEmpty(),
    channel = string("channel"),
    at = long("at") ?: 0L,
    watchedPercent = int("watchedPercent"),
    excluded = bool("excluded") == true,
    thumbnailUrl = string("thumbnailUrl"),
    viewings = int("viewings") ?: 1,
)

internal fun JsonObject.toPageTrace(): PageTrace = PageTrace(
    algorithm = string("algorithm").orEmpty(),
    builtAt = long("builtAt") ?: 0L,
    sources = objects("sources").map { TraceSource(it.string("kind").orEmpty(), it.int("received") ?: 0) },
    candidates = int("candidatesReceived") ?: 0,
    dropped = obj("dropped").orEmpty().mapNotNull { (why, count) ->
        (count as? JsonPrimitive)?.intOrNull?.let { why to it }
    }.toMap(),
    shown = int("shown") ?: 0,
    rows = objects("rows").map { row ->
        TraceRow(row.string("id").orEmpty(), row.string("title").orEmpty(), row.string("why").orEmpty(), row.int("videos") ?: 0)
    },
    scores = objects("scores").map { score ->
        TraceScore(
            url = score.string("url").orEmpty(),
            title = score.string("title").orEmpty(),
            channel = score.string("channel").orEmpty(),
            row = score.string("row").orEmpty(),
            total = score.double("total") ?: 0.0,
            factors = score.obj("factors").orEmpty().mapNotNull { (factor, value) ->
                (value as? JsonPrimitive)?.doubleOrNull?.let { factor to it }
            }.toMap(),
        )
    },
)

internal fun JsonArray.toFeedbackEntries(): List<FeedbackEntry> = filterIsInstance<JsonObject>().mapNotNull { entry ->
    FeedbackEntry(
        id = entry.string("id")?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null,
        kind = entry.string("kind").orEmpty(),
        verdict = entry.string("verdict").orEmpty(),
        comment = entry.string("comment"),
        reason = entry.string("reason"),
        title = entry.string("title"),
        channelName = entry.string("channelName"),
        videoUrl = entry.string("videoUrl"),
        thumbnailUrl = entry.string("thumbnailUrl"),
        algorithmId = entry.string("algorithmId").orEmpty(),
        createdAt = entry.long("createdAt") ?: 0L,
    )
}
