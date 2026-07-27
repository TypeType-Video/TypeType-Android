package dev.typetype.android.data.support

import android.os.Build
import dev.typetype.android.BuildConfig
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.BugApiErrorRequest
import dev.typetype.android.data.network.dto.BugCrashLogRequest
import dev.typetype.android.data.network.dto.BugReportContextRequest
import dev.typetype.android.data.network.dto.CreateBugReportRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import dev.typetype.android.domain.support.SupportReportCategory
import dev.typetype.android.domain.support.SupportReportDraft
import dev.typetype.android.domain.support.SupportReportReceipt
import dev.typetype.android.domain.support.SupportRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteSupportRepository @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val apiHolder: TypeTypeApiHolder,
) : SupportRepository {

    override suspend fun canSubmitReport(): Boolean {
        val scope = runCatching { activeAccountScope.require() }.getOrNull() ?: return false
        return accountDao.get(scope.serverId, scope.accountId)?.isGuest == false
    }

    override suspend fun submitReport(draft: SupportReportDraft): Result<SupportReportReceipt> = runCatching {
        val scope = activeAccountScope.require()
        val account = requireNotNull(accountDao.get(scope.serverId, scope.accountId)) {
            "Account not found"
        }
        check(!account.isGuest) { "Guest accounts cannot submit reports" }
        val response = withContext(Dispatchers.IO) {
            apiHolder.requireSupport(scope).createBugReport(draft.toRequest())
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
        val body = response.body() ?: error("The instance returned an empty report receipt")
        SupportReportReceipt(
            id = body.id,
            status = body.status,
            createdAtMillis = body.createdAt,
        )
    }

    private fun SupportReportDraft.toRequest(): CreateBugReportRequest {
        val description = description.trim()
        require(description.isNotEmpty()) { "Description is required" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) { "Description is too long" }
        return CreateBugReportRequest(
            category = category.wireValue,
            description = description,
            context = BugReportContextRequest(
                route = REPORT_ROUTE,
                timestamp = System.currentTimeMillis(),
                userAgent = nativeClientIdentity(),
                browserLanguage = Locale.getDefault().toLanguageTag().ifBlank { "und" },
                crashLogs = diagnostics.toCrashLogs(),
                apiErrors = diagnostics.toApiErrors(),
            ),
        )
    }

    private fun List<DiagnosticEntry>.toCrashLogs(): List<BugCrashLogRequest> =
        asSequence()
            .filter { it.method == APPLICATION_METHOD }
            .take(MAX_CRASH_LOGS)
            .map { entry ->
                BugCrashLogRequest(
                    message = entry.route,
                    timestamp = entry.timestampEpochMillis,
                )
            }
            .toList()

    private fun List<DiagnosticEntry>.toApiErrors(): List<BugApiErrorRequest> =
        asSequence()
            .filter { it.method != APPLICATION_METHOD && it.statusCode != null && it.statusCode >= 400 }
            .take(MAX_API_ERRORS)
            .map { entry ->
                BugApiErrorRequest(
                    requestId = entry.requestId,
                    endpoint = entry.route,
                    status = requireNotNull(entry.statusCode),
                    timestamp = entry.timestampEpochMillis,
                )
            }
            .toList()

    private fun nativeClientIdentity(): String =
        "TypeType Android/${BuildConfig.VERSION_NAME} " +
            "(Android ${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}; native)"

    private val SupportReportCategory.wireValue: String
        get() = when (this) {
            SupportReportCategory.Player -> "player"
            SupportReportCategory.AudioLanguage -> "audio_language"
            SupportReportCategory.Subtitles -> "subtitles"
            SupportReportCategory.Interface -> "ui"
            SupportReportCategory.Functionality -> "functionality"
        }

    private companion object {
        const val REPORT_ROUTE = "/android/settings/diagnostics"
        const val APPLICATION_METHOD = "APP"
        const val MAX_DESCRIPTION_LENGTH = 10_000
        const val MAX_CRASH_LOGS = 200
        const val MAX_API_ERRORS = 100
    }
}
