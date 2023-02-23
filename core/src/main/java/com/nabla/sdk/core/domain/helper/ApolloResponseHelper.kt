package com.nabla.sdk.core.domain.helper

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.watch
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.apollo.ApolloResponseExt.dataOrThrowOnError
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

@NablaInternal
public object ApolloResponseHelper {
    @NablaInternal
    public fun <T : Query.Data> ApolloCall<T>.watchAsCachedResponse(
        exceptionMapper: NablaExceptionMapper,
    ): Flow<Response<T>> {
        return makeCachedResponseWatcher(exceptionMapper) { fetchPolicy ->
            copy()
                .fetchPolicy(fetchPolicy)
                .watch(fetchThrows = true)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun <T : Query.Data> makeCachedResponseWatcher(
        exceptionMapper: NablaExceptionMapper,
        watcherCreator: (FetchPolicy) -> Flow<ApolloResponse<T>>,
    ): Flow<Response<T>> {
        var lastResponse: Response<T>? = null
        var lastError: NablaException? = null
        var fetchPolicy: FetchPolicy = FetchPolicy.CacheAndNetwork

        return flow { emit(fetchPolicy) }
            .flatMapLatest { watcherCreator(it) }
            .map { apolloResponse ->
                val error = lastError
                val response = Response(
                    isDataFresh = !apolloResponse.isFromCache,
                    refreshingState = when {
                        error != null && apolloResponse.isFromCache -> RefreshingState.ErrorWhileRefreshing(error)
                        !apolloResponse.isLast -> RefreshingState.Refreshing
                        else -> RefreshingState.Refreshed
                    },
                    data = apolloResponse.dataOrThrowOnError,
                )

                lastResponse = response

                return@map response
            }
            .retryWhen { cause, _ ->
                if (lastError == null && lastResponse != null) {
                    lastError = exceptionMapper.map(cause)
                    fetchPolicy = FetchPolicy.CacheOnly
                    return@retryWhen true
                }

                return@retryWhen false
            }
    }
}
