package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext
import com.benasher44.uuid.Uuid

object TypeAndUuidCacheKeyGenerator : CacheKeyGenerator {
    override fun cacheKeyForObject(
        obj: Map<String, Any?>,
        context: CacheKeyGeneratorContext
    ): CacheKey? {
        // TODO : Align with GQL ids scalar and name
        val typeName = obj["__typename"] as String
        val remoteId = obj["id"]
        val localId = obj["clientId"]
        if (localId is Uuid) {
            return CacheKey("$typeName-$localId")
        }
        return when (remoteId) {
            is Uuid -> CacheKey("$typeName-$remoteId")
            else -> null
        }
    }
}
