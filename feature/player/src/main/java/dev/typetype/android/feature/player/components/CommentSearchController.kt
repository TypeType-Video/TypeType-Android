package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentSearchPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class CommentSearchState(
    val query: String = "",
    val comments: List<Comment> = emptyList(),
    val scannedPages: Int = 0,
    val nextCursor: String? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
)

// Each search makes the server read up to 20 comment pages from the provider, so a search only
// starts when the viewer submits it, like the web client, never while typing.
@Stable
internal class CommentSearchController(
    private val scope: CoroutineScope,
    private val search: suspend (query: String, cursor: String?) -> Result<CommentSearchPage>,
) {
    var state by mutableStateOf(CommentSearchState())
        private set

    private var job: Job? = null

    fun submit(rawQuery: String) {
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isEmpty()) {
            clear()
            return
        }
        if (query == state.query && (state.isLoading || !state.failed)) return
        job?.cancel()
        state = CommentSearchState(query = query, isLoading = true)
        run(query, cursor = null)
    }

    fun searchFurther() {
        val current = state
        val cursor = current.nextCursor ?: return
        if (current.isLoading) return
        state = current.copy(isLoading = true, failed = false)
        run(current.query, cursor)
    }

    fun retry() {
        val current = state
        if (current.query.isEmpty() || current.isLoading) return
        state = current.copy(isLoading = true, failed = false)
        run(current.query, current.nextCursor)
    }

    fun clear() {
        job?.cancel()
        state = CommentSearchState()
    }

    private fun run(query: String, cursor: String?) {
        job = scope.launch {
            val result = search(query, cursor)
            if (state.query != query) return@launch
            state = result.fold(
                onSuccess = { page ->
                    state.copy(
                        comments = mergeCommentsById(state.comments, page.comments),
                        scannedPages = state.scannedPages + page.scannedPages,
                        nextCursor = page.nextCursor,
                        isLoading = false,
                    )
                },
                onFailure = { state.copy(isLoading = false, failed = true) },
            )
        }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 200
    }
}

@Composable
internal fun rememberCommentSearchController(
    videoUrl: String,
    search: suspend (videoUrl: String, query: String, cursor: String?) -> Result<CommentSearchPage>,
): CommentSearchController {
    val scope = rememberCoroutineScope()
    return remember(videoUrl, scope) {
        CommentSearchController(scope) { query, cursor -> search(videoUrl, query, cursor) }
    }
}
