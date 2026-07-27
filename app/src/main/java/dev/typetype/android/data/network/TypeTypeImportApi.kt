package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.PipePipeRestoreSummaryDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface TypeTypeImportApi {
    @Multipart
    @POST("restore/pipepipe")
    suspend fun restorePipePipe(
        @Query("timeMode") timeMode: String = "normalized",
        @Part file: MultipartBody.Part,
    ): Response<PipePipeRestoreSummaryDto>
}
