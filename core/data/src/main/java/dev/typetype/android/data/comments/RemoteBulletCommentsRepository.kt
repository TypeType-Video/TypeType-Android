package dev.typetype.android.data.comments

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.comments.BulletComment
import dev.typetype.android.domain.comments.BulletCommentsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteBulletCommentsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : BulletCommentsRepository {

    override suspend fun load(videoUrl: String): Result<List<BulletComment>> = try {
        Result.success(loadComments(videoUrl))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private suspend fun loadComments(videoUrl: String): List<BulletComment> {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).bulletComments(videoUrl)
        }
        response.requireSuccessfulResponse()
        val comments = response.body()?.comments ?: error("Empty bullet comments body")
        activeAccountScope.verify(scope)
        return comments.asSequence()
            .mapNotNull { it.toDomain() }
            .sortedBy(BulletComment::presentationTimeMillis)
            .take(MAX_COMMENTS)
            .toList()
    }

    private companion object {
        const val MAX_COMMENTS = 20_000
    }
}
