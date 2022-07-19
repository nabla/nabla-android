package com.nabla.sdk.core.data.apollo

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.FieldRecordMerger
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.MetadataGeneratorContext
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache

@VisibleForTesting
public object ApolloFactory {
    @VisibleForTesting
    @OptIn(ApolloExperimental::class)
    public fun configureBuilder(
        normalizedCacheFactory: NormalizedCacheFactory,
    ): ApolloClient.Builder = ApolloClient.Builder()
        .normalizedCache(
            normalizedCacheFactory = normalizedCacheFactory,
            cacheKeyGenerator = TypeAndUuidCacheKeyGenerator,
            metadataGenerator = ConversationsMetadataGenerator(),
            apolloResolver = FieldPolicyApolloResolver,
            recordMerger = FieldRecordMerger(ConversationsFieldMerger())
        )
}

@OptIn(ApolloExperimental::class)
private class ConversationsMetadataGenerator : MetadataGenerator {
    override fun metadataForObject(obj: Any?, context: MetadataGeneratorContext): Map<String, Any?> {
        return if (context.field.type.leafType().name == "ConversationsOutput") mapOf("type" to "ConversationsOutput") else emptyMap()
    }
}

@OptIn(ApolloExperimental::class)
private class ConversationsFieldMerger : FieldRecordMerger.FieldMerger {
    override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
        return if (incoming.metadata["type"] == "ConversationsOutput") {
            val existingValue = existing.value as Map<*, *>
            val existingList = existingValue["conversations"] as List<*>
            val incomingList = (incoming.value as Map<*, *>)["conversations"] as List<*>
            val mergedList: List<*> = existingList + incomingList
            val mergedFieldValue = existingValue.toMutableMap()
            mergedFieldValue["conversations"] = mergedList
            incoming.copy(value = mergedFieldValue)
        } else {
            incoming
        }
    }
}
