package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.PipePipeRestoreSummaryDto
import dev.typetype.android.data.network.dto.TypeTypeRestoreSummaryDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
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
}
