package dev.typetype.android.domain.allowlist

interface AllowedChannelsRepository {
    suspend fun allowedChannelUrls(): Result<Set<String>>
    suspend fun allowForEveryone(channelUrl: String, name: String, avatarUrl: String?): Result<Unit>
}
