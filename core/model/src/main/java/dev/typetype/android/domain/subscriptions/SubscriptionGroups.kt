package dev.typetype.android.domain.subscriptions

data class SubscriptionGroup(
    val id: String,
    val name: String,
    val channelCount: Int,
)

sealed interface SubscriptionFeedSelection {
    data object All : SubscriptionFeedSelection
    data object Ungrouped : SubscriptionFeedSelection
    data class Group(val id: String) : SubscriptionFeedSelection
}

interface SubscriptionGroupsRepository {
    suspend fun groups(): Result<List<SubscriptionGroup>>
    suspend fun create(name: String): Result<SubscriptionGroup>
    suspend fun rename(groupId: String, name: String): Result<Unit>
    suspend fun delete(groupId: String): Result<Unit>
    suspend fun groupIdsOf(channelUrl: String): Result<Set<String>>
    suspend fun setMember(groupId: String, channelUrl: String, member: Boolean): Result<Unit>
}
