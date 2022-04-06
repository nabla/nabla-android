package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.exception.CacheMissException

suspend fun <D : Operation.Data> ApolloClient.updateCache(
    operation: Operation<D>,
    update: suspend (D?) -> CacheUpdateOperation<D>,
) {
    val currentData = readFromCache(operation)
    val cacheUpdate = update(currentData)
    when (cacheUpdate) {
        is CacheUpdateOperation.Ignore -> Unit /* no-op */
        is CacheUpdateOperation.Write -> writeToCache(operation, cacheUpdate.data)
    }
}

suspend fun <D : Operation.Data> ApolloClient.readFromCache(
    operation: Operation<D>,
): D? = try {
    apolloStore.readOperation(operation)
} catch (cacheMissException: CacheMissException) {
    null
}

suspend fun <D : Operation.Data> ApolloClient.writeToCache(
    operation: Operation<D>,
    operationData: D,
) {
    apolloStore.writeOperation(operation, operationData)
}

sealed class CacheUpdateOperation<D> {
    data class Write<D>(val data: D) : CacheUpdateOperation<D>()
    class Ignore<D> : CacheUpdateOperation<D>()
}
