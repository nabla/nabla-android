package com.nabla.sdk.core.domain.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.exception.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.auth.throwOnStartIfNotAuthenticated
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@NablaInternal
public fun <T> makePaginatedFlow(
    paginationFlow: Flow<PaginatedList<T>>,
    loadMoreBlock: suspend () -> Unit,
    nablaExceptionMapper: NablaExceptionMapper,
    sessionClient: SessionClient,
): Flow<WatchPaginatedResponse<List<T>>> {
    val adaptedLoadMoreBlock = suspend {
        runCatchingCancellable {
            loadMoreBlock()
        }.mapFailureAsNablaException(nablaExceptionMapper)
    }
    return paginationFlow.throwOnStartIfNotAuthenticated(sessionClient)
        .map { paginatedList ->
            WatchPaginatedResponse(
                content = paginatedList.items,
                loadMore = if (paginatedList.hasMore) {
                    adaptedLoadMoreBlock
                } else null
            )
        }.catchAndRethrowAsNablaException(nablaExceptionMapper)
}
