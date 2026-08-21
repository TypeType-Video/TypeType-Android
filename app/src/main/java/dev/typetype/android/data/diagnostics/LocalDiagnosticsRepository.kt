package dev.typetype.android.data.diagnostics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.data.network.ApiBaseUrlHolder
import dev.typetype.android.data.network.NetworkRequestScope
import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import dev.typetype.android.domain.diagnostics.DiagnosticsRepository
import dev.typetype.android.domain.diagnostics.CrashRequestMetadata
import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

@Singleton
class LocalDiagnosticsRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val endpointHolder: ApiBaseUrlHolder,
    private val accountScopeStore: AccountScopeStore,
) : DiagnosticsRepository {
    private val directory = File(context.noBackupFilesDir, "diagnostics")
    private val lock = Any()
    private var latestRequest: ScopedDiagnosticEntry? = null
    private var latestSabr: ScopedDiagnosticEntry? = null

    override suspend fun listCurrent(): List<DiagnosticEntry> = withContext(Dispatchers.IO) {
        val scope = currentScope() ?: return@withContext emptyList()
        synchronized(lock) {
            val file = fileFor(scope)
            if (!file.isFile) return@synchronized emptyList()
            file.useLines { lines ->
                lines.mapNotNull(::decode)
                    .toList()
                    .takeLast(MAX_VISIBLE_ENTRIES)
                    .asReversed()
            }
        }
    }

    override suspend fun clearCurrent(): Unit = withContext(Dispatchers.IO) {
        val scope = currentScope() ?: return@withContext
        synchronized(lock) {
            fileFor(scope).delete()
        }
    }

    internal fun scopeFor(url: HttpUrl, requestScope: NetworkRequestScope? = null): DiagnosticScope? {
        if (requestScope != null) {
            val endpoint = dev.typetype.android.data.network.CurrentServerEndpoint(
                requestScope.serverId,
                requestScope.baseUrl,
            )
            val route = NetworkRouteClassifier.classify(endpoint, url) ?: return null
            return DiagnosticScope(requestScope.serverId, requestScope.accountId, route)
        }
        val endpoint = endpointHolder.currentEndpoint ?: return null
        val route = NetworkRouteClassifier.classify(endpoint, url) ?: return null
        return DiagnosticScope(
            serverId = endpoint.serverId,
            accountId = accountScopeStore.getCurrentAccountId(endpoint.serverId) ?: NO_ACCOUNT,
            route = route,
        )
    }

    internal fun record(
        scope: DiagnosticScope,
        method: String,
        statusCode: Int?,
        durationMillis: Long,
        requestId: String?,
        sabr: SabrDiagnosticDetail? = null,
    ) {
        val entry = DiagnosticEntry(
            timestampEpochMillis = System.currentTimeMillis(),
            method = method.takeIf { it in ALLOWED_METHODS } ?: "OTHER",
            route = scope.route,
            statusCode = statusCode,
            durationMillis = durationMillis.coerceIn(0, MAX_DURATION_MILLIS),
            requestId = requestId?.takeIf(REQUEST_ID_PATTERN::matches),
            sabr = sabr,
        )
        synchronized(lock) {
            val scopedEntry = ScopedDiagnosticEntry(scope.serverId, scope.accountId, entry)
            latestRequest = scopedEntry
            if (entry.sabr != null) latestSabr = scopedEntry
            directory.mkdirs()
            val file = fileFor(scope)
            file.appendText(encode(entry) + "\n", Charsets.UTF_8)
            if (file.length() > MAX_FILE_BYTES) trim(file)
        }
    }

    internal fun recordLocalEvent(
        route: String,
        timestampEpochMillis: Long = System.currentTimeMillis(),
        statusCode: Int? = null,
        requestId: String? = null,
    ) {
        if (route !in LOCAL_EVENT_ROUTES) return
        val scope = currentScope()?.copy(route = route) ?: return
        val entry = DiagnosticEntry(
            timestampEpochMillis = timestampEpochMillis,
            method = LOCAL_METHOD,
            route = route,
            statusCode = statusCode?.takeIf { it in 100..599 },
            durationMillis = 0,
            requestId = requestId?.takeIf(REQUEST_ID_PATTERN::matches),
        )
        synchronized(lock) {
            directory.mkdirs()
            val file = fileFor(scope)
            file.appendText(encode(entry) + "\n", Charsets.UTF_8)
            if (file.length() > MAX_FILE_BYTES) trim(file)
        }
    }

    internal fun currentCrashContext(): CrashDiagnosticContext {
        val scope = currentScope() ?: return CrashDiagnosticContext(null, null)
        return synchronized(lock) {
            val request = latestRequest?.takeIf { it.matches(scope) }?.entry
            val sabr = latestSabr?.takeIf { it.matches(scope) }?.entry?.sabr
            CrashDiagnosticContext(
                lastRequest = request?.let {
                    CrashRequestMetadata(it.method, it.route, it.requestId)
                },
                lastSabr = sabr,
            )
        }
    }

    private fun currentScope(): DiagnosticScope? {
        val endpoint = endpointHolder.currentEndpoint ?: return null
        return DiagnosticScope(
            serverId = endpoint.serverId,
            accountId = accountScopeStore.getCurrentAccountId(endpoint.serverId) ?: NO_ACCOUNT,
            route = "",
        )
    }

    private fun fileFor(scope: DiagnosticScope): File {
        val key = "${scope.serverId}\u0000${scope.accountId}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(directory, "$digest.log")
    }

    private fun trim(file: File) {
        val bytes = file.readBytes()
        var start = (bytes.size - RETAINED_FILE_BYTES).coerceAtLeast(0)
        while (start < bytes.size && bytes[start] != '\n'.code.toByte()) start += 1
        file.writeBytes(bytes.copyOfRange((start + 1).coerceAtMost(bytes.size), bytes.size))
    }

    private fun encode(entry: DiagnosticEntry): String = listOf(
        entry.timestampEpochMillis,
        entry.method,
        entry.route,
        entry.statusCode ?: 0,
        entry.durationMillis,
        entry.requestId.orEmpty(),
        entry.sabr?.let(SabrDiagnosticDetailCodec::encode).orEmpty(),
    ).joinToString("\t")

    private fun decode(line: String): DiagnosticEntry? {
        val fields = line.split('\t', limit = 7)
        if (fields.size !in 6..7) return null
        val route = fields[2].takeIf { it.startsWith('/') && it.length <= 40 } ?: return null
        return DiagnosticEntry(
            timestampEpochMillis = fields[0].toLongOrNull() ?: return null,
            method = fields[1].takeIf {
                it in ALLOWED_METHODS || it == "OTHER" ||
                    it == LOCAL_METHOD || it == LEGACY_APPLICATION_METHOD
            }
                ?: return null,
            route = route,
            statusCode = fields[3].toIntOrNull()?.takeIf { it in 100..599 },
            durationMillis = fields[4].toLongOrNull() ?: return null,
            requestId = fields[5].takeIf(REQUEST_ID_PATTERN::matches),
            sabr = fields.getOrNull(6)
                ?.takeIf(String::isNotEmpty)
                ?.let(SabrDiagnosticDetailCodec::decode),
        )
    }

    internal data class DiagnosticScope(
        val serverId: String,
        val accountId: String,
        val route: String,
    )

    private data class ScopedDiagnosticEntry(
        val serverId: String,
        val accountId: String,
        val entry: DiagnosticEntry,
    ) {
        fun matches(scope: DiagnosticScope): Boolean =
            serverId == scope.serverId && accountId == scope.accountId
    }

    private companion object {
        const val NO_ACCOUNT = "__no_account__"
        const val MAX_FILE_BYTES = 256L * 1024L
        const val RETAINED_FILE_BYTES = 128 * 1024
        const val MAX_VISIBLE_ENTRIES = 500
        const val MAX_DURATION_MILLIS = 15 * 60 * 1_000L
        val ALLOWED_METHODS = setOf("DELETE", "GET", "PATCH", "POST", "PUT")
        val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9_-]{8,64}")
        const val LOCAL_METHOD = "LOCAL"
        const val LEGACY_APPLICATION_METHOD = "APP"
        val LOCAL_EVENT_ROUTES = setOf(
            "/app/crash",
            "/app/exit/anr",
            "/app/exit/crash",
            "/app/exit/low-memory",
            "/app/exit/system",
            "/app/exit/user",
            "/network/available",
            "/network/changed",
            "/network/lost",
            "/subscriptions/feed/contract",
            "/subscriptions/feed/decode",
            "/subscriptions/feed/pagination",
            "/subscriptions/feed/persistence",
            "/subscriptions/feed/ready",
            "/subscriptions/feed/server",
        )
    }
}
