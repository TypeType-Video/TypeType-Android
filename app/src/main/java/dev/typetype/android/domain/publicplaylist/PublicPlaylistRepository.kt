package dev.typetype.android.domain.publicplaylist

interface PublicPlaylistRepository {
    suspend fun load(url: String, nextPage: String? = null): Result<PublicPlaylistPage>
}
