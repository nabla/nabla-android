package com.nabla.sdk.core.data.file

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

internal interface FileService {
    @Multipart
    @POST("file")
    suspend fun upload(@Part file: MultipartBody.Part): List<String>
}
