package com.nabla.sdk.core.data.file

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

internal interface FileService {
    @Multipart
    @POST(UPLOAD_FILE_PATH)
    suspend fun upload(
        @Part purpose: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): List<String>

    companion object {
        const val UPLOAD_FILE_PATH = "v1/upload/patient"
    }
}
