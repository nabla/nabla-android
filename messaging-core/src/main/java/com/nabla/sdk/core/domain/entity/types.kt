package com.nabla.sdk.core.domain.entity

/**
 * Abstract platform-agnostic types that can be mapped from and into each platform types (e.g. jvm or android's URI, Date, UUID, etc.)
 */

@JvmInline
value class Uri(val uri: String)

@JvmInline
value class Id(val id: String)

fun String.toId() = Id(this)
