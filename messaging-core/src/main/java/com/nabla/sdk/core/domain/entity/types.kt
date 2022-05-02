package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

/*
 Abstract platform-agnostic types that can be mapped from and into each platform types (e.g. jvm or android's URI, Date, UUID, etc.)
*/

@JvmInline
value class Uri(val uri: String) {
    override fun toString(): String = uri
}

@JvmInline
value class StringId(val value: String) {
    override fun toString(): String = value
}

internal fun String.toId() = StringId(this)
internal fun String.asUuid(): Uuid = Uuid.fromString(this)
