package dev.typetype.android.data.recommendations

import dev.typetype.android.domain.recommendations.AlgorithmCatalog
import dev.typetype.android.domain.recommendations.AlgorithmInfo
import dev.typetype.android.domain.recommendations.AlgorithmParameter
import dev.typetype.android.domain.recommendations.AlgorithmTraits
import dev.typetype.android.domain.recommendations.DataFamily
import dev.typetype.android.domain.recommendations.InterestGroup
import dev.typetype.android.domain.recommendations.RecommenderPreferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.toCatalog(): AlgorithmCatalog = AlgorithmCatalog(
    defaultId = string("default_algorithm"),
    algorithms = array("algorithms").mapNotNull { (it as? JsonObject)?.toAlgorithm() },
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
        parameters = array("parameters").mapNotNull { (it as? JsonObject)?.toParameter() },
    )
}

private fun JsonObject.toParameter(): AlgorithmParameter? {
    val id = string("id") ?: return null
    val group = string("group").orEmpty()
    val label = string("label") ?: id
    val help = string("help")
    val featured = this["featured"]?.jsonPrimitive?.booleanOrNull == true
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
            default = this["default"]?.jsonPrimitive?.booleanOrNull == true,
        )
        else -> null
    }
}

internal fun JsonObject.toPreferences(): RecommenderPreferences = RecommenderPreferences(
    enabled = this["enabled"]?.jsonPrimitive?.booleanOrNull == true,
    algorithmId = string("algorithmId"),
    parameters = obj("parameters").orEmpty().mapNotNull { (key, value) ->
        val primitive = value as? JsonPrimitive ?: return@mapNotNull null
        val parsed: Any = primitive.booleanOrNull ?: primitive.doubleOrNull ?: return@mapNotNull null
        key to parsed
    }.toMap(),
    pinnedGroups = strings("pinnedGroups"),
    mutedGroups = strings("mutedGroups"),
    customInterests = strings("customInterests"),
    unusedData = strings("unusedData"),
)

internal fun JsonObject.withPreferences(preferences: RecommenderPreferences): JsonObject =
    JsonObject(
        toMutableMap().apply {
            put("enabled", JsonPrimitive(preferences.enabled))
            put("algorithmId", preferences.algorithmId?.let(::JsonPrimitive) ?: JsonNull)
            put(
                "parameters",
                JsonObject(
                    preferences.parameters.mapValues { (_, value) ->
                        when (value) {
                            is Boolean -> JsonPrimitive(value)
                            is Number -> JsonPrimitive(value)
                            else -> JsonPrimitive(value.toString())
                        }
                    },
                ),
            )
            put("pinnedGroups", preferences.pinnedGroups.toJsonArray())
            put("mutedGroups", preferences.mutedGroups.toJsonArray())
            put("customInterests", preferences.customInterests.toJsonArray())
            put("unusedData", preferences.unusedData.toJsonArray())
        },
    )

internal fun JsonObject.toInterests(): List<InterestGroup> = array("groups").mapNotNull { element ->
    val group = element as? JsonObject ?: return@mapNotNull null
    InterestGroup(
        id = group.string("id") ?: return@mapNotNull null,
        label = group.string("label").orEmpty(),
        weight = group.double("weight") ?: 0.0,
        pinned = group["pinned"]?.jsonPrimitive?.booleanOrNull == true,
        muted = group["muted"]?.jsonPrimitive?.booleanOrNull == true,
    )
}

internal fun JsonArray.toDataFamilies(): List<DataFamily> = mapNotNull { element ->
    val family = element as? JsonObject ?: return@mapNotNull null
    DataFamily(
        id = family.string("id") ?: return@mapNotNull null,
        count = family.int("count") ?: 0,
        used = family["use"]?.jsonPrimitive?.booleanOrNull != false,
    )
}

private fun List<String>.toJsonArray(): JsonArray = JsonArray(map(::JsonPrimitive))

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

private fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()

private fun JsonObject.strings(key: String): List<String> =
    array(key).mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
