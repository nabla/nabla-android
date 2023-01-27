package com.nabla.sdk.core.domain.entity

import androidx.annotation.CheckResult
import com.nabla.sdk.core.annotation.NablaInternal

/**
 * Typically used in Flow to wrap paginated content and the callback to load the next page.
 *
 * Each time there's a change to content (be it new pages added or already loaded content changing)
 * the flow will emit a new instance of [PaginatedContent] with the new content and the new callback.
 *
 * You are expected to keep a reference to and call the latest [loadMore] callback, also avoid keeping a reference to older ones.
 *
 * @param content the data being watched, grows after each new page successful loading.
 * @param loadMore the callback to trigger loading of the next page, will eventually result in a new emission in the Flow.
 */
public data class PaginatedContent<T> @NablaInternal public constructor(
    val content: T,
    val loadMore: (@CheckResult suspend () -> Result<Unit>)?,
)
