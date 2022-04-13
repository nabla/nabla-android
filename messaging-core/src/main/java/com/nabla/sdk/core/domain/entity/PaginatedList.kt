package com.nabla.sdk.core.domain.entity

internal data class PaginatedList<T>(
    val items: List<T>,
    val hasMore: Boolean,
) {
    companion object {
        fun <T> empty() = PaginatedList<T>(emptyList(), false)
    }
}
