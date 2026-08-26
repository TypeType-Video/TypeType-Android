package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.PipePipeRestoreSummaryDto
import dev.typetype.android.data.network.dto.PortabilityApplyRequestDto
import dev.typetype.android.data.network.dto.PortabilityExportRequestDto
import dev.typetype.android.data.network.dto.PortabilityFormatDto
import dev.typetype.android.data.network.dto.PortabilityJobDto
import dev.typetype.android.data.network.dto.TypeTypeRestoreSummaryDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutCommitRequestDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutJobStatusDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutPreviewDto
import dev.typetype.android.data.network.dto.YoutubeTakeoutReportDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface TypeTypeImportApi {
    @GET("backup/typetype")
    suspend fun exportTypeType(
        @Query("categories") categories: String,
    ): Response<ResponseBody>

    @GET("portability/formats")
    suspend fun portabilityFormats(): Response<List<PortabilityFormatDto>>

    @POST("portability/exports")
    suspend fun startPortabilityExport(
        @Body request: PortabilityExportRequestDto,
    ): Response<PortabilityJobDto>

    @Multipart
    @POST("portability/imports")
    suspend fun startPortabilityImport(
        @Query("format") format: String,
        @Part file: MultipartBody.Part,
    ): Response<PortabilityJobDto>

    @POST("portability/jobs/{jobId}/apply")
    suspend fun applyPortabilityImport(
        @Path("jobId") jobId: String,
        @Body request: PortabilityApplyRequestDto,
    ): Response<PortabilityJobDto>

    @GET("portability/jobs/{jobId}")
    suspend fun portabilityJob(@Path("jobId") jobId: String): Response<PortabilityJobDto>

    @POST("portability/jobs/{jobId}/cancel")
    suspend fun cancelPortabilityJob(@Path("jobId") jobId: String): Response<PortabilityJobDto>

    @DELETE("portability/jobs/{jobId}")
    suspend fun deletePortabilityJob(@Path("jobId") jobId: String): Response<Unit>

    @Streaming
    @GET("portability/jobs/{jobId}/artifact")
    suspend fun portabilityArtifact(@Path("jobId") jobId: String): Response<ResponseBody>

    @GET("portability/jobs/{jobId}/report")
    suspend fun portabilityReport(@Path("jobId") jobId: String): Response<ResponseBody>

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
