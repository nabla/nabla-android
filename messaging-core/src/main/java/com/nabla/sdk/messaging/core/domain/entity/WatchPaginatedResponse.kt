package com.nabla.sdk.messaging.core.domain.entity

import androidx.annotation.CheckResult

data class WatchPaginatedResponse<T> internal constructor(
    val content: T,
    val loadMore: (@CheckResult suspend () -> Result<Unit>)?,
)
