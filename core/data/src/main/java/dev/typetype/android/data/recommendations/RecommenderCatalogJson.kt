package dev.typetype.android.data.recommendations

import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.AlgorithmInfo
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import dev.typetype.android.domain.recommendations.AlgorithmTraits
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.toCatalog(): AlgorithmCatalog = AlgorithmCatalog(
    defaultId = string("default_algorithm"),
    algorithms = objects("algorithms").mapNotNull { it.toAlgorithm() },
)

private fun JsonObject.toAlgorithm(): AlgorithmInfo? {
    val id = string("id") ?: return null
    val guide = obj("guide")
    val traits = guide?.obj("traits")
    return AlgorithmInfo(
        id = id,
        name = string("name") ?: id,
        description = string("description").orEmpty(),
        forYouIf = guide?.string("for_you_if").orEmpty(),
        youWillSee = guide?.string("you_will_see").orEmpty(),
        goodToKnow = guide?.string("good_to_know").orEmpty(),
        traits = AlgorithmTraits(
            personal = traits?.int("personal") ?: 0,
            discovery = traits?.int("discovery") ?: 0,
            variety = traits?.int("variety") ?: 0,
        ),
        parameters = objects("parameters").mapNotNull { it.toParameter() },
        uses = strings("uses"),
        sources = strings("sources"),
        sourceUrl = string("source_url")?.takeIf { it.startsWith("https://") },
    )
}

private fun JsonObject.toParameter(): AlgorithmParameter? {
    val id = string("id") ?: return null
    val group = string("group").orEmpty()
    val label = string("label") ?: id
    val help = string("help")
    val featured = bool("featured") == true
    return when (string("type")) {
        "number" -> AlgorithmParameter.Number(
            id = id,
            group = group,
            label = label,
            help = help,
            featured = featured,
            min = double("min") ?: 0.0,
            max = double("max") ?: 1.0,
            step = double("step") ?: 1.0,
            default = double("default") ?: 0.0,
            unit = string("unit"),
        )
        "boolean" -> AlgorithmParameter.Toggle(
            id = id,
            group = group,
            label = label,
            help = help,
            featured = featured,
            default = bool("default") == true,
        )
        else -> null
    }
}

internal fun JsonObject.toPreferences(): RecommenderPreferences = RecommenderPreferences(
    enabled = bool("enabled") == true,
    algorithmId = string("algorithmId"),
    parameters = obj("parameters").orEmpty().mapNotNull { (key, value) ->
        val primitive = value as? JsonPrimitive ?: return@mapNotNull null
        val parsed: Any = primitive.booleanOrNull ?: primitive.doubleOrNull ?: return@mapNotNull null
        key to parsed
    }.toMap(),
    pinnedGroups = strings("pinnedGroups"),
    mutedGroups = strings("mutedGroups"),
    ignoredSignals = strings("ignoredSignals"),
    sessionResetAt = long("sessionResetAt"),
    customInterests = strings("customInterests"),
    unusedData = strings("unusedData"),
    excludedItems = strings("excludedItems"),
    retentionDays = obj("retentionDays").orEmpty().mapNotNull { (key, value) ->
        (value as? JsonPrimitive)?.intOrNull?.let { key to it }
    }.toMap(),
)

internal fun JsonObject.withPreferences(preferences: RecommenderPreferences): JsonObject =
    JsonObject(
        toMutableMap().apply {
            put("enabled", JsonPrimitive(preferences.enabled))
            put("algorithmId", preferences.algorithmId?.let(::JsonPrimitive) ?: JsonNull)
            put("parameters", JsonObject(preferences.parameters.mapValues { (_, value) -> value.toJsonValue() }))
            put("pinnedGroups", preferences.pinnedGroups.toJsonArray())
            put("mutedGroups", preferences.mutedGroups.toJsonArray())
            put("ignoredSignals", preferences.ignoredSignals.toJsonArray())
            put("sessionResetAt", preferences.sessionResetAt?.let(::JsonPrimitive) ?: JsonNull)
            put("customInterests", preferences.customInterests.toJsonArray())
            put("unusedData", preferences.unusedData.toJsonArray())
            put("excludedItems", preferences.excludedItems.toJsonArray())
            put("retentionDays", JsonObject(preferences.retentionDays.mapValues { (_, days) -> JsonPrimitive(days) }))
        },
    )

private fun Any.toJsonValue(): JsonPrimitive = when (this) {
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    else -> JsonPrimitive(toString())
}
