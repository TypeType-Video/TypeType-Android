package dev.typetype.android.feature.subscriptions

import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.library.LibraryCollectionSyncState

internal fun SubscriptionsState.withSyncState(
    sync: LibraryCollectionSyncState?,
    errorMapper: UserErrorMapper,
): SubscriptionsState {
    val failure = sync?.takeIf { it.failedWriteCount > 0 }?.let {
        errorMapper.message(
            failureCode = it.writeFailureCode,
            statusCode = it.writeFailureStatusCode,
            fallbackRes = R.string.library_changes_sync_failed,
        )
    }
    return copy(
        syncErrorMessage = failure,
        syncRequestId = sync?.writeRequestId,
        lastSuccessfulSyncAtMillis = sync?.lastSuccessAtMillis,
        pendingWriteCount = sync?.pendingWriteCount ?: 0,
        failedWriteCount = sync?.failedWriteCount ?: 0,
    )
}
