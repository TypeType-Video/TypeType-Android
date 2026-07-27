package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.BugReportCreateResponse
import dev.typetype.android.data.network.dto.CreateBugReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TypeTypeSupportApi {
    @POST("bug-reports")
    suspend fun createBugReport(
        @Body request: CreateBugReportRequest,
    ): Response<BugReportCreateResponse>
}
