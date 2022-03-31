package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGeneratorContext

object TypeAndUuidCacheKeyGenerator : CacheKeyGenerator {
    override fun cacheKeyForObject(
        obj: Map<String, Any?>,
        context: CacheKeyGeneratorContext
    ): CacheKey? {
        // TODO : Align with GQL ids scalar and name
        return when (val uuid = obj["uuid"]) {
            is String -> CacheKey("${obj["__typename"]}-$uuid")
            else -> null
        }
    }
}
