package com.nabla.sdk.core.data.apollo

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache

@VisibleForTesting
public object ApolloFactory {
    @VisibleForTesting
    public fun configureBuilder(
        normalizedCacheFactory: NormalizedCacheFactory
    ): ApolloClient.Builder = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = normalizedCacheFactory,
            cacheKeyGenerator = TypeAndUuidCacheKeyGenerator
        )
}
