package dev.typetype.android.data.recommendations

import dev.typetype.android.domain.recommendations.RecommenderPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderJsonTest {
    private fun obj(text: String) = Json.parseToJsonElement(text) as JsonObject
    private fun array(text: String) = Json.parseToJsonElement(text) as JsonArray

    @Test
    fun preferencesRoundTripKeepsEveryFieldAndUnknownOnes() {
        val server = obj(
            """{"enabled":true,"algorithmId":"signals","parameters":{"w":0.5,"shorts":false},
            "ignoredSignals":["channel:a"],"sessionResetAt":1700,"excludedItems":["v1"],
            "retentionDays":{"history":30},"future":"keep"}""",
        )
        val parsed = server.toPreferences()
        assertEquals(listOf("channel:a"), parsed.ignoredSignals)
        assertEquals(1700L, parsed.sessionResetAt)
        assertEquals(mapOf("history" to 30), parsed.retentionDays)
        assertEquals(0.5, parsed.parameters["w"])
        assertEquals(false, parsed.parameters["shorts"])

        val saved = server.withPreferences(parsed.copy(retentionDays = emptyMap(), sessionResetAt = null))
        assertEquals("keep", saved["future"]?.jsonPrimitive?.content)
        assertEquals(RecommenderPreferences().retentionDays, saved.toPreferences().retentionDays)
        assertNull(saved.toPreferences().sessionResetAt)
        assertEquals(listOf("v1"), saved.toPreferences().excludedItems)
    }

    @Test
    fun catalogReadsTransparencyFieldsAndRejectsUnsafeSourceLinks() {
        val catalog = obj(
            """{"default_algorithm":"a","algorithms":[
            {"id":"a","name":"A","uses":["history"],"sources":["trending"],"source_url":"https://example.org/a"},
            {"id":"b","name":"B","source_url":"javascript:alert(1)"}]}""",
        ).toCatalog()
        assertEquals(listOf("history"), catalog.algorithms[0].uses)
        assertEquals("https://example.org/a", catalog.algorithms[0].sourceUrl)
        assertNull(catalog.algorithms[1].sourceUrl)
    }

    @Test
    fun profileCountsSourcesAndKeepsExamples() {
        val profile = obj(
            """{"groups":[{"id":"group:1","label":"Space","weight":2.5,"from":["a","b"],"pinned":true,
            "examples":[{"url":"u","title":"T","thumbnailUrl":"https://i/t.jpg"}]}],
            "channels":[{"id":"c","name":"Chan","weight":1,"from":["x"],"avatarUrl":"https://i/a.jpg"}],
            "session_boosts":[{"ref":"r","label":"L","strength":1,"expires_at":99}]}""",
        ).toProfile()
        val group = profile.groups.single()
        assertEquals(2, group.basedOn)
        assertTrue(group.pinned)
        assertTrue(group.pinnable)
        assertFalse(group.custom)
        assertEquals("T", group.examples.single().title)
        assertEquals(1, profile.channels.single().basedOn)
        assertEquals(99L, profile.sessionBoosts.single().expiresAt)
    }

    @Test
    fun dataCategoriesKeepCollectAndItems() {
        val data = array(
            """[{"id":"history","count":3,"use":false,"retentionDays":90,"collect":true,
            "items":[{"id":"v","title":"Video","at":5,"watchedPercent":40,"excluded":true,"viewings":2}]},
            {"id":"blocked","count":0}]""",
        ).toDataCategories()
        assertFalse(data[0].used)
        assertEquals(90, data[0].retentionDays)
        assertEquals(true, data[0].collect)
        assertEquals(2, data[0].items.single().viewings)
        assertTrue(data[1].used)
        assertNull(data[1].collect)
    }

    @Test
    fun traceAndFeedbackAreRead() {
        val trace = obj(
            """{"algorithm":"signals","builtAt":10,"sources":[{"kind":"related","detail":"d","received":4}],
            "candidatesReceived":40,"dropped":{"seen":3},"shown":20,
            "rows":[{"id":"r","title":"Row","why":"because","videos":5}],
            "scores":[{"url":"u","title":"t","channel":"c","row":"r","total":1.5,"factors":{"fresh":0.5}}]}""",
        ).toPageTrace()
        assertEquals(40, trace.candidates)
        assertEquals(mapOf("seen" to 3), trace.dropped)
        assertEquals(0.5, trace.scores.single().factors["fresh"])

        val feedback = array(
            """[{"id":"f1","kind":"video","verdict":"not_for_me","reason":"seen","title":"T","algorithmId":"a","createdAt":7},
            {"id":"","kind":"page","verdict":"good"}]""",
        ).toFeedbackEntries()
        assertEquals(1, feedback.size)
        assertEquals("seen", feedback.single().reason)
    }
}
