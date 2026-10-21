package dev.typetype.android.data.appupdate

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppUpdateInstallLaunch {
    data object InstallerOpened : AppUpdateInstallLaunch
    data object PermissionRequested : AppUpdateInstallLaunch
}

@Singleton
class AndroidAppUpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun validate(file: File, release: AppUpdateRelease) {
        if (!file.isFile || file.length() != release.apkSizeBytes || file.length() < APK_HEADER_SIZE) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_PACKAGE)
        }
        val currentPackage = context.packageName
        if (currentPackage != EXPECTED_PACKAGE_NAME) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_PACKAGE)
        }
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(file.absolutePath, packageInfoFlags())
            ?: throw AppUpdateFailure(AppUpdateFailureReason.INVALID_PACKAGE)
        if (archive.packageName != EXPECTED_PACKAGE_NAME || archive.versionName != release.versionName) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_PACKAGE)
        }
        val archiveCode = archive.versionCodeLong()
        val installed = packageManager.getPackageInfo(currentPackage, packageInfoFlags())
        if (archiveCode <= installed.versionCodeLong()) {
            throw AppUpdateFailure(AppUpdateFailureReason.DOWNGRADE_BLOCKED)
        }
        if (release.versionCode?.let { it != archiveCode } == true) {
            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_PACKAGE)
        }
        if (!signingMatches(packageManager, installed, archive)) {
            throw AppUpdateFailure(AppUpdateFailureReason.SIGNATURE_MISMATCH)
        }
    }

    fun launch(file: File): AppUpdateInstallLaunch {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return AppUpdateInstallLaunch.PermissionRequested
        }
        val apkUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".updates",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return AppUpdateInstallLaunch.InstallerOpened
    }

    private fun packageInfoFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    private fun signingMatches(
        packageManager: PackageManager,
        installed: PackageInfo,
        archive: PackageInfo,
    ): Boolean {
        val archiveSigners = signerDigests(archive)
        if (archiveSigners.isEmpty()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return archiveSigners.all { digest ->
                val signerBytes = signerBytes(archive).firstOrNull { sha256(it) == digest }
                signerBytes != null && packageManager.hasSigningCertificate(
                    context.packageName,
                    signerBytes,
                    PackageManager.CERT_INPUT_RAW_X509,
                )
            }
        }
        return archiveSigners == signerDigests(installed)
    }

    private fun signerDigests(info: PackageInfo): Set<String> =
        signerBytes(info).mapTo(mutableSetOf(), ::sha256)

    private fun signerBytes(info: PackageInfo): List<ByteArray> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.map { it.toByteArray() }.orEmpty()
        }

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }

    @Suppress("DEPRECATION")
    private fun PackageInfo.versionCodeLong(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    private companion object {
        const val EXPECTED_PACKAGE_NAME = "dev.typetype.android"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val APK_HEADER_SIZE = 4L
    }
}
