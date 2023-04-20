package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext

internal object TypeAndUuidCacheKeyGenerator : CacheKeyGenerator {
    override fun cacheKeyForObject(
        obj: Map<String, Any?>,
        context: CacheKeyGeneratorContext,
    ): CacheKey? {
        // Values provided here are scalar, as the json payload would be
        val typeName = obj["__typename"]
        val remoteId = obj["id"] ?: obj["uuid"]
        val localId = obj["clientId"]
        if (localId is String) {
            return CacheKey("$typeName-$localId")
        }
        return when (remoteId) {
            is String -> CacheKey("$typeName-$remoteId")
            else -> null
        }
    }
}
