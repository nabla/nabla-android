package com.nabla.sdk.core.domain.helper

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.apollo.ApolloResponseExt.dataOrThrowOnError
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun <T : Query.Data> makeCachedResponseWatcher(
        exceptionMapper: NablaExceptionMapper,
        watcherCreator: (FetchPolicy) -> Flow<ApolloResponse<T>>,
    ): Flow<Response<T>> {
        var lastResponse: Response<T>? = null

        return watcherCreator(FetchPolicy.CacheAndNetwork)
            .map { apolloResponse ->
                val response = Response(
                    isDataFresh = !apolloResponse.isFromCache,
                    refreshingState = when {
                        !apolloResponse.isLast -> RefreshingState.Refreshing
                        else -> RefreshingState.Refreshed
                    },
                    data = apolloResponse.dataOrThrowOnError,
                )

                lastResponse = response

                return@map response
            }.catch { cause: Throwable ->
                when (cause) {
                    // As we are using FetchPolicy.CacheAndNetwork policy, we either get:
                    is ApolloNetworkException -> { // if cache hit and network error
                        val response = lastResponse
                        if (response != null) {
                            emit(
                                response.copy(
                                    refreshingState = RefreshingState.ErrorWhileRefreshing(
                                        exceptionMapper.map(cause)
                                    )
                                )
                            )
                        } else {
                            throw cause
                        }
                    }
                    is ApolloCompositeException -> { // if cache miss and network error
                        lastResponse = null
                        throw cause
                    }
                    else -> {
                        throw cause
                    }
                }
            }
    }
}
