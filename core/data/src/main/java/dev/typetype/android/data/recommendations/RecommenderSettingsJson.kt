package dev.typetype.android.data.recommendations

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.withRecommenderEnabled(enabled: Boolean): JsonObject =
    JsonObject(toMutableMap().apply { put("enabled", JsonPrimitive(enabled)) })

internal fun JsonObject.recommenderEnabled(): Boolean =
    this["enabled"]?.jsonPrimitive?.booleanOrNull == true
