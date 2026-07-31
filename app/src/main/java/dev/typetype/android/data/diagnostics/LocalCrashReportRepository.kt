package dev.typetype.android.data.diagnostics

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.data.network.ApiBaseUrlHolder
import dev.typetype.android.domain.diagnostics.CrashReport
import dev.typetype.android.domain.diagnostics.CrashReportRepository
import dev.typetype.android.domain.server.ServerRepository
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class LocalCrashReportRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val endpointHolder: ApiBaseUrlHolder,
    private val accountScopeStore: AccountScopeStore,
    private val serverRepository: ServerRepository,
    private val json: Json,
) : CrashReportRepository {
    private val directory = File(context.noBackupFilesDir, "diagnostics/crashes")
    private val lock = Any()

    override fun recordCurrent(report: CrashReport): Boolean = synchronized(lock) {
        write(fileFor(immediateScope()), report, acknowledged = false)
    }

    override suspend fun pendingCurrent(): CrashReport? = withContext(Dispatchers.IO) {
        currentFiles().mapNotNull(::read)
            .filterNot(DecodedCrashReport::acknowledged)
            .maxByOrNull { it.report.occurredAtEpochMillis }
            ?.report
    }

    override suspend fun latestCurrent(): CrashReport? = withContext(Dispatchers.IO) {
        currentFiles().mapNotNull(::read)
            .maxByOrNull { it.report.occurredAtEpochMillis }
            ?.report
    }

    override suspend fun acknowledgeCurrent(): Unit = withContext(Dispatchers.IO) {
        val files = currentFiles()
        synchronized(lock) {
            files.forEach { file ->
                val decoded = read(file) ?: return@forEach
                if (!decoded.acknowledged) write(file, decoded.report, acknowledged = true)
            }
        }
    }

    override suspend fun clearCurrent(): Unit = withContext(Dispatchers.IO) {
        val files = currentFiles()
        synchronized(lock) { files.forEach(File::delete) }
    }

    private fun write(file: File, report: CrashReport, acknowledged: Boolean): Boolean {
        return try {
            directory.mkdirs()
            val atomicFile = AtomicFile(file)
            val output = atomicFile.startWrite()
            try {
                output.write(CrashReportCodec.encode(json, report, acknowledged).toByteArray())
                atomicFile.finishWrite(output)
                true
            } catch (failure: IOException) {
                atomicFile.failWrite(output)
                false
            } catch (failure: RuntimeException) {
                atomicFile.failWrite(output)
                false
            }
        } catch (failure: IOException) {
            false
        } catch (failure: RuntimeException) {
            false
        }
    }

    private fun read(file: File): DecodedCrashReport? = try {
        if (!file.isFile || file.length() > MAX_REPORT_BYTES) null
        else CrashReportCodec.decode(json, AtomicFile(file).readFully().toString(Charsets.UTF_8))
    } catch (failure: IOException) {
        null
    } catch (failure: RuntimeException) {
        null
    }

    private suspend fun currentFiles(): List<File> {
        val server = serverRepository.observeCurrentServer().first()
        val scope = server?.let {
            StorageScope(it.id, accountScopeStore.getCurrentAccountId(it.id) ?: NO_ACCOUNT)
        }
        return listOfNotNull(scope?.let(::fileFor), fileFor(UNSCOPED_SCOPE)).distinct()
    }

    private fun immediateScope(): StorageScope {
        val serverId = endpointHolder.currentEndpoint?.serverId ?: return UNSCOPED_SCOPE
        return StorageScope(
            serverId,
            accountScopeStore.getCurrentAccountId(serverId) ?: NO_ACCOUNT,
        )
    }

    private fun fileFor(scope: StorageScope): File {
        val key = "${scope.serverId}\u0000${scope.accountId}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(directory, "$digest.json")
    }

    private data class StorageScope(val serverId: String, val accountId: String)

    private companion object {
        const val NO_ACCOUNT = "__no_account__"
        const val MAX_REPORT_BYTES = 128L * 1024L
        val UNSCOPED_SCOPE = StorageScope("__no_server__", NO_ACCOUNT)
    }
}
