package dev.typetype.android.feature.settings.recommender

import dev.typetype.android.domain.recommendations.RecommenderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommenderEditsTest {
    @Test
    fun choosingAnAlgorithmStartsFromItsDefaults() {
        val preferences = RecommenderPreferences(algorithmId = "a", parameters = mapOf("w" to 0.4))
        val chosen = preferences.choose("b")
        assertEquals("b", chosen.algorithmId)
        assertEquals(emptyMap<String, Any>(), chosen.parameters)
    }

    @Test
    fun pinningAndHidingExcludeEachOther() {
        val pinned = RecommenderPreferences().togglePinned("group:1")
        assertEquals(listOf("group:1"), pinned.pinnedGroups)
        val hidden = pinned.toggleMuted("group:1")
        assertEquals(listOf("group:1"), hidden.mutedGroups)
        assertEquals(emptyList<String>(), hidden.pinnedGroups)
    }

    @Test
    fun interestsAreTrimmedDeduplicatedAndCapped() {
        val added = RecommenderPreferences().addInterest("  Japanese   cooking ")
        assertEquals(listOf("Japanese cooking"), added?.customInterests)
        assertNull(added?.addInterest("japanese COOKING"))
        assertNull(RecommenderPreferences().addInterest("   "))
        val full = RecommenderPreferences(customInterests = (1..MAX_CUSTOM_INTERESTS).map { "s$it" })
        assertNull(full.addInterest("more"))
        assertEquals(MAX_CUSTOM_INTERESTS - 1, full.removeInterest("S1").customInterests.size)
    }

    @Test
    fun retentionNullMeansNoLimit() {
        val limited = RecommenderPreferences().withRetention("history", 30)
        assertEquals(mapOf("history" to 30), limited.retentionDays)
        assertEquals(emptyMap<String, Int>(), limited.withRetention("history", null).retentionDays)
    }

    @Test
    fun dataTogglesFlip() {
        val preferences = RecommenderPreferences().toggleUse("history").toggleExcluded("v1").toggleIgnored("c")
        assertEquals(listOf("history"), preferences.unusedData)
        assertEquals(listOf("v1"), preferences.excludedItems)
        assertEquals(listOf("c"), preferences.ignoredSignals)
        assertEquals(emptyList<String>(), preferences.toggleUse("history").unusedData)
    }
}
