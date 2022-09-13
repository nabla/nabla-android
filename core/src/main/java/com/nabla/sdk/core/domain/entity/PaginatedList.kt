package com.nabla.sdk.core.domain.entity

public data class PaginatedList<out T>(
    val items: List<T>,
    val hasMore: Boolean,
) {
    public companion object {
        public fun <T> empty(): PaginatedList<T> = PaginatedList(emptyList(), false)
    }
}
