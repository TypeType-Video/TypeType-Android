package dev.typetype.android.data.recommendations

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }

internal fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()

internal fun JsonObject.objects(key: String): List<JsonObject> = array(key).filterIsInstance<JsonObject>()

internal fun JsonObject.strings(key: String): List<String> =
    array(key).mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }

internal fun List<String>.toJsonArray(): JsonArray = JsonArray(map(::JsonPrimitive))
