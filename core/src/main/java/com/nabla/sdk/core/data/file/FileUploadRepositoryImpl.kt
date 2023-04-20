package com.nabla.sdk.core.data.file

import android.content.Context
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.asUuid
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.InputStream

internal class FileUploadRepositoryImpl constructor(
    private val fileService: FileService,
    appContext: Context,
    private val uuidGenerator: UuidGenerator,
) : FileUploadRepository {

    private val contentResolver = appContext.contentResolver

    override suspend fun uploadFile(localPath: Uri, fileName: String?, mimeType: MimeType): Uuid {
        val fileInputStream = contentResolver.openInputStream(localPath.toAndroidUri())
            ?: throw IOException("Unable to open input stream from uri: $localPath")

        fileInputStream.use { inputStream ->
            val response = fileService.upload(
                body = MultipartBody.Builder(boundary = uuidGenerator.generate().toString())
                    .addFormDataPart("purpose", "MESSAGE")
                    .addFormDataPart(
                        "file",
                        fileName ?: uuidGenerator.generate().toString(),
                        buildUploadRequestBody(inputStream, mimeType.stringRepresentation),
                    )
                    .build(),
            )
            return response.first().asUuid()
        }
    }

    private fun buildUploadRequestBody(
        inputStream: InputStream,
        mimeType: String?,
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return mimeType?.toMediaTypeOrNull()
            }

            override fun writeTo(sink: BufferedSink) {
                sink.writeAll(inputStream.source())
            }
        }
    }
}
