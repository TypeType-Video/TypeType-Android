package dev.typetype.android.domain.recommendations

sealed interface AlgorithmParameter {
    val id: String
    val group: String
    val label: String
    val help: String?
    val featured: Boolean

    data class Number(
        override val id: String,
        override val group: String,
        override val label: String,
        override val help: String?,
        override val featured: Boolean,
        val min: Double,
        val max: Double,
        val step: Double,
        val default: Double,
        val unit: String?,
    ) : AlgorithmParameter

    data class Toggle(
        override val id: String,
        override val group: String,
        override val label: String,
        override val help: String?,
        override val featured: Boolean,
        val default: Boolean,
    ) : AlgorithmParameter
}

data class AlgorithmTraits(val personal: Int, val discovery: Int, val variety: Int)

data class AlgorithmInfo(
    val id: String,
    val name: String,
    val description: String,
    val forYouIf: String,
    val youWillSee: String,
    val goodToKnow: String,
    val traits: AlgorithmTraits,
    val parameters: List<AlgorithmParameter>,
)

data class AlgorithmCatalog(val defaultId: String?, val algorithms: List<AlgorithmInfo>)

data class RecommenderPreferences(
    val enabled: Boolean = false,
    val algorithmId: String? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val pinnedGroups: List<String> = emptyList(),
    val mutedGroups: List<String> = emptyList(),
    val customInterests: List<String> = emptyList(),
    val unusedData: List<String> = emptyList(),
)

data class InterestGroup(
    val id: String,
    val label: String,
    val weight: Double,
    val pinned: Boolean,
    val muted: Boolean,
)

data class DataFamily(val id: String, val count: Int, val used: Boolean)

interface RecommenderRepository {
    suspend fun catalog(locale: String): Result<AlgorithmCatalog>
    suspend fun preferences(): Result<RecommenderPreferences>
    suspend fun save(preferences: RecommenderPreferences): Result<RecommenderPreferences>
    suspend fun interests(locale: String): Result<List<InterestGroup>>
    suspend fun dataFamilies(): Result<List<DataFamily>>
}
