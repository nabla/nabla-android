package com.nabla.sdk.core.domain.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.CoroutineHelpers.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.CoroutineHelpers.mapFailureAsNablaException
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.helper.AuthHelper.throwOnStartIfNotAuthenticatable
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@NablaInternal
public object PaginationHelper {
    @NablaInternal
    public fun <T> Flow<PaginatedList<T>>.wrapAsPaginatedContent(
        loadMoreBlock: suspend () -> Unit,
        nablaExceptionMapper: NablaExceptionMapper,
        sessionClient: SessionClient,
    ): Flow<PaginatedContent<List<T>>> {
        val adaptedLoadMoreBlock = suspend {
            runCatchingCancellable {
                loadMoreBlock()
            }.mapFailureAsNablaException(nablaExceptionMapper)
        }
        return this
            .throwOnStartIfNotAuthenticatable(sessionClient)
            .map { paginatedList ->
                PaginatedContent(
                    content = paginatedList.items,
                    loadMore = if (paginatedList.hasMore) {
                        adaptedLoadMoreBlock
                    } else null
                )
            }.catchAndRethrowAsNablaException(nablaExceptionMapper)
    }

    @NablaInternal
    public fun <T> Flow<Response<PaginatedList<T>>>.wrapAsResponsePaginatedContent(
        loadMoreBlock: suspend () -> Unit,
        nablaExceptionMapper: NablaExceptionMapper,
        sessionClient: SessionClient,
    ): Flow<Response<PaginatedContent<List<T>>>> {
        val adaptedLoadMoreBlock = suspend {
            runCatchingCancellable {
                loadMoreBlock()
            }.mapFailureAsNablaException(nablaExceptionMapper)
        }
        return this
            .throwOnStartIfNotAuthenticatable(sessionClient)
            .map { response ->
                Response(
                    isDataFresh = response.isDataFresh,
                    refreshingState = response.refreshingState,
                    data = PaginatedContent(
                        content = response.data.items,
                        loadMore = if (response.data.hasMore) {
                            adaptedLoadMoreBlock
                        } else null
                    )
                )
            }.catchAndRethrowAsNablaException(nablaExceptionMapper)
    }
}
