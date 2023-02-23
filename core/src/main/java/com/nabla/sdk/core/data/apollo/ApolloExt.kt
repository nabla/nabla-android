package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.exception.CacheMissException
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public object ApolloExt {
    @NablaInternal
    public suspend fun <D : Operation.Data> ApolloClient.updateCache(
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

    @NablaInternal
    public suspend fun <D : Operation.Data> ApolloClient.readFromCache(
        operation: Operation<D>,
    ): D? = try {
        apolloStore.readOperation(operation)
    } catch (cacheMissException: CacheMissException) {
        null
    }

    internal suspend fun <D : Operation.Data> ApolloClient.writeToCache(
        operation: Operation<D>,
        operationData: D,
    ) {
        apolloStore.writeOperation(operation, operationData)
    }

    @NablaInternal
    public sealed class CacheUpdateOperation<D> {

        @NablaInternal
        public data class Write<D>(val data: D) : CacheUpdateOperation<D>()

        @NablaInternal
        public class Ignore<D> : CacheUpdateOperation<D>()
    }
}
