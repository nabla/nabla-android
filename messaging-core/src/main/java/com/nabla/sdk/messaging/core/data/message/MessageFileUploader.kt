package com.nabla.sdk.messaging.core.data.message

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.InvalidMessageException
import com.nabla.sdk.messaging.core.domain.entity.Message

internal class MessageFileUploader constructor(
    private val fileUploadRepository: FileUploadRepository,
) {
    suspend fun uploadFile(mediaMessage: Message.Media<*, *>): Uuid {
        val mediaSource = mediaMessage.mediaSource
        if (mediaSource !is FileSource.Local) {
            throw InvalidMessageException("Can't send a media message with a media source that is not local")
        }

        return fileUploadRepository.uploadFile(mediaSource.fileLocal.uri, mediaMessage.fileName, mediaMessage.mimeType)
    }
}
