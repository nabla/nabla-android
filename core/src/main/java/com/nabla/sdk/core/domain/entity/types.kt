package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal

/*
 Abstract platform-agnostic types that can be mapped from and into each platform types (e.g. jvm or android's URI, Date, UUID, etc.)
*/

@JvmInline
public value class Uri(public val uri: String) {
    override fun toString(): String = uri
}

@JvmInline
@NablaInternal
public value class StringId(public val value: String) {
    override fun toString(): String = value
}

internal fun String.toId() = StringId(this)
internal fun String.asUuid(): Uuid = Uuid.fromString(this)
