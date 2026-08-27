package dev.typetype.android.feature.player

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.typetype.android.data.comments.CommentsPagingSource
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
internal fun playerCommentsFlow(
    repository: CommentsRepository,
    videoUrls: Flow<String?>,
    scope: CoroutineScope,
): Flow<PagingData<Comment>> = videoUrls
    .flatMapLatest { url ->
        if (url.isNullOrBlank()) flowOf(PagingData.empty())
        else Pager(
            config = COMMENTS_PAGING_CONFIG,
            pagingSourceFactory = { CommentsPagingSource(repository, url) },
        ).flow
    }
    .cachedIn(scope)

internal val COMMENTS_PAGING_CONFIG = PagingConfig(
    pageSize = 30,
    initialLoadSize = 60,
    prefetchDistance = 10,
    maxSize = 240,
    enablePlaceholders = false,
)
