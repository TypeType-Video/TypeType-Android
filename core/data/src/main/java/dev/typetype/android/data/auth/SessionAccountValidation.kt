package dev.typetype.android.data.auth

class SessionAccountMismatchException : IllegalStateException()

internal fun requireExpectedAccount(expectedAccountId: String?, actualAccountId: String) {
    if (expectedAccountId != null && expectedAccountId != actualAccountId) {
        throw SessionAccountMismatchException()
    }
}
