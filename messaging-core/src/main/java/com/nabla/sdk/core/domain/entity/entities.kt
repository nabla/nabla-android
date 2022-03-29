package com.nabla.sdk.core.domain.entity

// There is not url type in kotlin common atm. We can add url extensions to handle conversion with
// platform specific types like JVM or Android URL.
typealias Url = String

typealias Id = String

sealed interface User {
    data class Provider(
        val id: Id,
        val avatar: Attachment?,
        val firstName: String,
        val lastName: String,
        val title: String?,
        val prefix: String?,
    ) : User

    data class Patient(
        val id: Id,
        val avatar: Attachment?,
        val username: String,
        val labels: List<String>,
    ) : User
}

data class Attachment(
    val id: Id,
    val url: Url,
    val mimeType: MimeType,
    val thumbnailUrl: Url,
)

sealed class MimeType {
    abstract val rawValue: String
    data class Generic(override val rawValue: String): MimeType()
}
