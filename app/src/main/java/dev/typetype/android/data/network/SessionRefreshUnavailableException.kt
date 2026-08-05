package dev.typetype.android.data.network

import dev.typetype.android.core.error.CodedFailure
import java.io.IOException

class SessionRefreshUnavailableException(
    override val statusCode: Int?,
    override val failureCode: String?,
    override val requestId: String?,
    cause: Throwable? = null,
) : IOException("The session could not be refreshed temporarily", cause), CodedFailure
