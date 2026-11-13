package dev.typetype.android.data.usersettings

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.usersettings.SeekbarIconRepository
import dev.typetype.android.domain.usersettings.SeekbarIconUpload
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class RemoteSeekbarIconRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val userSettings: UserSettingsRepository,
) : SeekbarIconRepository {

    override suspend fun upload(icon: SeekbarIconUpload): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val body = icon.bytes.toRequestBody(icon.mediaType.toMediaTypeOrNull())
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).uploadSeekbarIcon(body) }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        userSettings.refresh().getOrThrow()
    }

    override suspend fun delete(): Result<Unit> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) { apiHolder.require(scope).deleteSeekbarIcon() }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        userSettings.refresh().getOrThrow()
    }
}
