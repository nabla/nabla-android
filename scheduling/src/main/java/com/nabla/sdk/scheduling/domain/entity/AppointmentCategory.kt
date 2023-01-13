package com.nabla.sdk.scheduling.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import kotlin.time.Duration

@NablaInternal
public data class AppointmentCategory(
    val id: AppointmentCategoryId,
    val name: String,
    val callDuration: Duration,
)

@JvmInline
public value class AppointmentCategoryId(public val value: Uuid)
