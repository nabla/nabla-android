package com.nabla.sdk.core.domain.entity


// There is not url type in kotlin common atm. We can add url extensions to handle conversion with
// platform specific types like JVM or Android URL.
typealias Url = String

typealias ProviderId = String
data class Provider(
    val id: ProviderId,
    val avatar: AttachmentId?,
)

typealias AttachmentId = String
data class Attachment(
    val id: AttachmentId,
    val url: Url,
    val mimeType: MimeType,
    val thumbnailUrl: Url,
)

sealed class MimeType {
    abstract val rawValue: String
    data class Generic(override val rawValue: String): MimeType()
}

typealias PatientId = String
data class Patient(
    val id: PatientId,
    val labels: List<String>,
)

sealed class UserId {
    val rawValue: String = when (this) {
        is Patient -> id
        is Provider -> id
    }
    data class Patient(val id: PatientId): UserId()
    data class Provider(val id: ProviderId): UserId()
}