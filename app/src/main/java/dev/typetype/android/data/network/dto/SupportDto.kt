package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBugReportRequest(
    val category: String,
    val description: String,
    val context: BugReportContextRequest,
)

@Serializable
data class BugReportContextRequest(
    val route: String,
    val timestamp: Long,
    val userAgent: String,
    val browserLanguage: String,
    val crashLogs: List<BugCrashLogRequest> = emptyList(),
    val apiErrors: List<BugApiErrorRequest> = emptyList(),
)

@Serializable
data class BugCrashLogRequest(
    val message: String,
    val timestamp: Long,
)

@Serializable
data class BugApiErrorRequest(
    val requestId: String? = null,
    val endpoint: String,
    val status: Int,
    val timestamp: Long,
)

@Serializable
data class BugReportCreateResponse(
    val id: String,
    val status: String,
    val createdAt: Long,
)
