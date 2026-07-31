package dev.typetype.android.data.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.BuildConfig
import dev.typetype.android.data.network.ApiBaseUrlHolder
import dev.typetype.android.domain.diagnostics.CrashReportRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class ApplicationLifecycleDiagnostics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalDiagnosticsRepository,
    private val crashReports: CrashReportRepository,
    private val endpointHolder: ApiBaseUrlHolder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        installCrashHandler()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scope.launch {
                endpointHolder.awaitCurrent()
                collectHistoricalExits()
            }
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashReports.recordCurrent(
                    CrashReportSanitizer.create(
                        throwable = throwable,
                        environment = currentEnvironment(),
                        diagnostics = repository.currentCrashContext(),
                    ),
                )
                repository.recordLocalEvent("/app/crash")
            } finally {
                if (previous != null) {
                    previous.uncaughtException(thread, throwable)
                } else {
                    Process.killProcess(Process.myPid())
                    exitProcess(10)
                }
            }
        }
    }

    private fun currentEnvironment(): CrashEnvironment = CrashEnvironment(
        appVersion = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE.toLong(),
        androidVersion = Build.VERSION.RELEASE,
        apiLevel = Build.VERSION.SDK_INT,
        deviceManufacturer = Build.MANUFACTURER,
        deviceModel = Build.MODEL,
    )

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun collectHistoricalExits() {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val marker = File(context.noBackupFilesDir, EXIT_MARKER_FILE)
        val lastTimestamp = marker.takeIf(File::isFile)?.readText()?.toLongOrNull() ?: 0L
        val exits = manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXIT_RECORDS)
            .filter { it.timestamp > lastTimestamp }
            .sortedBy(ApplicationExitInfo::getTimestamp)
        exits.forEach { info ->
            exitRoute(info.reason)?.let { route ->
                repository.recordLocalEvent(route, info.timestamp)
            }
        }
        exits.maxOfOrNull(ApplicationExitInfo::getTimestamp)?.let { marker.writeText(it.toString()) }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun exitRoute(reason: Int): String? = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "/app/exit/anr"
        ApplicationExitInfo.REASON_CRASH, ApplicationExitInfo.REASON_CRASH_NATIVE -> "/app/exit/crash"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "/app/exit/low-memory"
        ApplicationExitInfo.REASON_USER_REQUESTED, ApplicationExitInfo.REASON_USER_STOPPED -> "/app/exit/user"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
        ApplicationExitInfo.REASON_OTHER,
        ApplicationExitInfo.REASON_SIGNALED,
        -> "/app/exit/system"
        else -> null
    }

    private companion object {
        const val EXIT_MARKER_FILE = "application_exit_marker"
        const val MAX_EXIT_RECORDS = 16
    }
}
