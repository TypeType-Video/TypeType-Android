package dev.typetype.android.domain.appupdate

import java.io.IOException

enum class AppUpdateFailureReason {
    NETWORK,
    INVALID_RELEASE,
    INVALID_CHECKSUM,
    INVALID_PACKAGE,
    DOWNGRADE_BLOCKED,
    SIGNATURE_MISMATCH,
    INSTALLER_UNAVAILABLE,
}

class AppUpdateFailure(
    val reason: AppUpdateFailureReason,
    cause: Throwable? = null,
) : IOException(reason.name, cause)
