package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.PipePipeRestoreSummaryDto
import dev.typetype.android.data.network.dto.TypeTypeRestoreSummaryDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutCommitRequestDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutJobStatusDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutPreviewDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutReportDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface TypeTypeImportApi {
    @GET("backup/typetype")
    suspend fun exportTypeType(
        @Query("categories") categories: String,
    ): Response<ResponseBody>

    @Multipart
    @POST("restore/typetype")
    suspend fun restoreTypeType(
        @Part file: MultipartBody.Part,
    ): Response<TypeTypeRestoreSummaryDto>

    @Multipart
    @POST("restore/pipepipe")
    suspend fun restorePipePipe(
        @Query("timeMode") timeMode: String = "normalized",
        @Part file: MultipartBody.Part,
    ): Response<PipePipeRestoreSummaryDto>

    @Multipart
    @POST("imports/youtube-takeout")
    suspend fun uploadYoutubeTakeout(
        @Part archive: MultipartBody.Part,
    ): Response<YoutubeTakeoutJobStatusDto>

    @GET("imports/youtube-takeout/{jobId}")
    suspend fun youtubeTakeoutStatus(
        @Path("jobId") jobId: String,
    ): Response<YoutubeTakeoutJobStatusDto>

    @GET("imports/youtube-takeout/{jobId}/preview")
    suspend fun youtubeTakeoutPreview(
        @Path("jobId") jobId: String,
    ): Response<YoutubeTakeoutPreviewDto>

    @POST("imports/youtube-takeout/{jobId}/commit")
    suspend fun commitYoutubeTakeout(
        @Path("jobId") jobId: String,
        @Body request: YoutubeTakeoutCommitRequestDto,
    ): Response<YoutubeTakeoutJobStatusDto>

    @GET("imports/youtube-takeout/{jobId}/report")
    suspend fun youtubeTakeoutReport(
        @Path("jobId") jobId: String,
    ): Response<YoutubeTakeoutReportDto>
}
