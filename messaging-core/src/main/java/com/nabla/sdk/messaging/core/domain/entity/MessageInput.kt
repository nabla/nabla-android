package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.FileUpload

public sealed class MessageInput {
    /**
     * Input for a text message, containing only the text to send.
     */
    public data class Text(val text: String) : MessageInput()
    public sealed class Media : MessageInput() {
        /**
         * Input for an image message, containing the local image to send.
         */
        public data class Image(val mediaSource: FileSource.Local<FileLocal.Image, FileUpload.Image>) : Media()

        /**
         * Input for a document message, containing the local document to send.
         */
        public data class Document(val mediaSource: FileSource.Local<FileLocal.Document, FileUpload.Document>) : Media()

        /**
         * Input for a voice message, containing the local audio file to send.
         */
        public data class Audio(val mediaSource: FileSource.Local<FileLocal.Audio, FileUpload.Audio>) : Media()
    }
}
