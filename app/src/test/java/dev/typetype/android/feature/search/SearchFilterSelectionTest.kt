package dev.typetype.android.feature.search

import dev.typetype.android.domain.search.SearchFilterGroup
import dev.typetype.android.domain.search.SearchFilterOption
import dev.typetype.android.domain.search.SearchFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchFilterSelectionTest {
    @Test
    fun serverGroupsReplaceLegacySortOptions() {
        val serverGroup = group("date", multiSelect = true, option("week"))
        val filters = SearchFilters(
            content = emptyList(),
            sort = listOf(option("views")),
            groups = listOf(serverGroup),
        )

        assertEquals(listOf(serverGroup), filters.resolvedGroups())
    }

    @Test
    fun legacySortOptionsBecomeSingleSelectGroupWithDefault() {
        val groups = SearchFilters(
            content = emptyList(),
            sort = listOf(option("relevance"), option("views")),
        ).resolvedGroups()

        assertEquals(1, groups.size)
        assertFalse(groups.single().multiSelect)
        assertTrue(groups.single().options.first().isDefault)
    }

    @Test
    fun sanitizingKeepsKnownNonDefaultOptionsInGroupOrder() {
        val groups = listOf(
            group("sort", false, defaultOption("relevance"), option("views"), option("date")),
            group("duration", true, option("short"), option("long")),
        )

        val selected = sanitizeSearchFilters(
            groups = groups,
            selected = listOf("unknown", "date", "views", "relevance", "long", "short"),
        )

        assertEquals(listOf("views", "short", "long"), selected)
    }

    @Test
    fun singleSelectToggleReplacesSelectionAndDefaultClearsIt() {
        val group = group("sort", false, defaultOption("relevance"), option("views"), option("date"))

        val replaced = toggleSearchFilter(listOf(group), listOf("views"), "sort", "date")
        val cleared = toggleSearchFilter(listOf(group), replaced, "sort", "relevance")

        assertEquals(listOf("date"), replaced)
        assertEquals(emptyList<String>(), cleared)
    }

    @Test
    fun multiSelectToggleAddsAndRemovesIndependently() {
        val group = group("duration", true, option("short"), option("long"))

        val added = toggleSearchFilter(listOf(group), listOf("short"), "duration", "long")
        val removed = toggleSearchFilter(listOf(group), added, "duration", "short")

        assertEquals(listOf("short", "long"), added)
        assertEquals(listOf("long"), removed)
    }

    @Test
    fun defaultIsSelectedOnlyWhenGroupHasNoActiveValue() {
        val fallback = defaultOption("all")
        val active = option("live")
        val group = group("type", false, fallback, active)

        assertTrue(isSearchFilterSelected(group, fallback, emptyList()))
        assertFalse(isSearchFilterSelected(group, fallback, listOf("live")))
        assertTrue(isSearchFilterSelected(group, active, listOf("live")))
    }

    private fun option(value: String) = SearchFilterOption(value, value)

    private fun defaultOption(value: String) = SearchFilterOption(value, value, isDefault = true)

    private fun group(
        key: String,
        multiSelect: Boolean,
        vararg options: SearchFilterOption,
    ) = SearchFilterGroup(key, key, multiSelect, options.toList())
}
