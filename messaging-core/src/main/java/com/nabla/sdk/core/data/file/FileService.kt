package com.nabla.sdk.core.data.file

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

internal interface FileService {
    @POST(UPLOAD_FILE_PATH)
    suspend fun upload(
        @Body body: RequestBody,
    ): List<String>

    companion object {
        const val UPLOAD_FILE_PATH = "v1/upload/patient"
    }
}
