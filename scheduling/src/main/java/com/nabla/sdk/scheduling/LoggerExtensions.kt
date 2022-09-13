package com.nabla.sdk.scheduling

import com.nabla.sdk.core.domain.boundary.Logger

internal class SchedulingDomain {
    val UI = "Scheduling-UI"
}

internal val Logger.Companion.SCHEDULING_DOMAIN: SchedulingDomain
    get() = SchedulingDomain()
