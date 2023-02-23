package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public data class PaginatedList<out T>(
    val items: List<T>,
    val hasMore: Boolean,
) {
    @VisibleForTesting
    public companion object {
    }
}
