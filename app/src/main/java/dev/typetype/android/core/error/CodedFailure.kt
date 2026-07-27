package dev.typetype.android.core.error

interface CodedFailure {
    val failureCode: String?
    val requestId: String?
    val statusCode: Int?
}
