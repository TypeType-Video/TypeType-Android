package dev.typetype.android.data.recommendations

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderSettingsJsonTest {
    @Test
    fun togglingPersonalizationPreservesEveryOtherServerSetting() {
        val settings = buildJsonObject {
            put("enabled", false)
            put("algorithmId", "signals")
            put("customInterests", buildJsonArray { add("astronomy") })
            put("futureServerField", "keep me")
        }

        val updated = settings.withRecommenderEnabled(true)

        assertTrue(updated.recommenderEnabled())
        assertEquals("signals", updated["algorithmId"]?.jsonPrimitive?.content)
        assertEquals("astronomy", updated["customInterests"]?.jsonArray?.first()?.jsonPrimitive?.content)
        assertEquals("keep me", updated["futureServerField"]?.jsonPrimitive?.content)
        assertFalse(settings.recommenderEnabled())
    }
}
