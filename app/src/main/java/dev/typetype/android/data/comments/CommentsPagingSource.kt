package dev.typetype.android.data.comments

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository

class CommentsPagingSource(
    private val repository: CommentsRepository,
    private val videoUrl: String,
) : PagingSource<String, Comment>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        val nextpage = params.key
        return repository.loadPage(videoUrl = videoUrl, nextpage = nextpage).fold(
            onSuccess = { page ->
                LoadResult.Page(
                    data = page.comments,
                    prevKey = null,
                    nextKey = page.nextpage,
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    override fun getRefreshKey(state: PagingState<String, Comment>): String? = null
}
