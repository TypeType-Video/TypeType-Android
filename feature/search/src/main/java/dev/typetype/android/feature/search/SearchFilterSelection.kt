package dev.typetype.android.feature.search

import dev.typetype.android.domain.search.SearchFilterGroup
import dev.typetype.android.domain.search.SearchFilterOption
import dev.typetype.android.domain.search.SearchFilters

internal fun SearchFilters.resolvedGroups(): List<SearchFilterGroup> {
    if (groups.isNotEmpty()) return groups
    if (sort.isEmpty()) return emptyList()
    val hasDefault = sort.any(SearchFilterOption::isDefault)
    return listOf(
        SearchFilterGroup(
            key = "legacy-sort",
            label = "Sort by",
            multiSelect = false,
            options = sort.mapIndexed { index, option ->
                option.copy(isDefault = option.isDefault || (!hasDefault && index == 0))
            },
        ),
    )
}

internal fun sanitizeSearchFilters(
    groups: List<SearchFilterGroup>,
    selected: List<String>,
): List<String> {
    val requested = selected.toSet()
    return groups.flatMap { group ->
        val matches = group.options.filter { it.value in requested && !it.isDefault }
        (if (group.multiSelect) matches else matches.take(1)).map(SearchFilterOption::value)
    }
}

internal fun toggleSearchFilter(
    groups: List<SearchFilterGroup>,
    selected: List<String>,
    groupKey: String,
    optionValue: String,
): List<String> {
    val group = groups.firstOrNull { it.key == groupKey }
        ?: return sanitizeSearchFilters(groups, selected)
    val option = group.options.firstOrNull { it.value == optionValue }
        ?: return sanitizeSearchFilters(groups, selected)
    val groupValues = group.options.mapTo(mutableSetOf(), SearchFilterOption::value)
    val next = selected.filterNot(groupValues::contains).toMutableList()
    if (group.multiSelect) {
        next += selected.filter { it in groupValues && it != option.value }
    }
    if (!option.isDefault && option.value !in selected) next += option.value
    return sanitizeSearchFilters(groups, next)
}

internal fun activeSearchFilterOptions(
    groups: List<SearchFilterGroup>,
    selected: List<String>,
): List<SearchFilterOption> {
    val active = sanitizeSearchFilters(groups, selected).toSet()
    return groups.flatMap { group -> group.options.filter { it.value in active } }
}

internal fun isSearchFilterSelected(
    group: SearchFilterGroup,
    option: SearchFilterOption,
    selected: List<String>,
): Boolean = if (!option.isDefault) {
    option.value in selected
} else {
    group.options.none { !it.isDefault && it.value in selected }
}
