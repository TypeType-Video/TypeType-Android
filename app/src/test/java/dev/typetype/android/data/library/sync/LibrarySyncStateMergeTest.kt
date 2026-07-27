package dev.typetype.android.data.library.sync

import dev.typetype.android.domain.library.LibraryCollection
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySyncStateMergeTest {
    @Test
    fun exposesPendingAndFailedWritesWithoutLosingRefreshState() {
        val states = mergeSyncStates(
            refreshRows = listOf(
                LibrarySyncEntity(
                    serverId = "server",
                    accountId = "account",
                    collection = "favorites",
                    refreshGeneration = 2L,
                    lastAttemptAtMillis = 10L,
                    lastSuccessAtMillis = 11L,
                    lastFailureAtMillis = null,
                    failureCode = null,
                    failureStatusCode = null,
                    requestId = null,
                ),
            ),
            mutationRows = listOf(
                mutation("a", MUTATION_PENDING, 20L),
                mutation("b", MUTATION_FAILED, 30L, "permission_denied", 403, "req-b"),
            ),
        )

        val favorites = requireNotNull(states[LibraryCollection.Favorites])
        assertEquals(11L, favorites.lastSuccessAtMillis)
        assertEquals(1, favorites.pendingWriteCount)
        assertEquals(1, favorites.failedWriteCount)
        assertEquals("permission_denied", favorites.writeFailureCode)
        assertEquals(403, favorites.writeFailureStatusCode)
        assertEquals("req-b", favorites.writeRequestId)
    }

    private fun mutation(
        target: String,
        state: String,
        updatedAt: Long,
        failureCode: String? = null,
        failureStatus: Int? = null,
        requestId: String? = null,
    ) = LibraryMutationEntity(
        serverId = "server",
        accountId = "account",
        mutationKey = target,
        collection = "favorites",
        kind = "favorite",
        targetId = target,
        parentId = null,
        desiredPresent = true,
        title = "",
        thumbnailUrl = "",
        durationSeconds = 0L,
        channelName = "",
        channelUrl = "",
        channelAvatarUrl = "",
        viewCount = 0L,
        sessionGeneration = 1L,
        mutationVersion = 1L,
        state = state,
        createdAtMillis = updatedAt,
        updatedAtMillis = updatedAt,
        lastAttemptAtMillis = updatedAt,
        attemptCount = 1,
        failureCode = failureCode,
        failureStatusCode = failureStatus,
        requestId = requestId,
    )
}
