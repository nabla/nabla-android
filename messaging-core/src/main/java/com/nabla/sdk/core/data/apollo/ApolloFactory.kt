package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache

internal object ApolloFactory {
    internal fun configureBuilder(
        normalizedCacheFactory: NormalizedCacheFactory
    ) = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = normalizedCacheFactory,
            cacheKeyGenerator = TypeAndUuidCacheKeyGenerator
        )
}
