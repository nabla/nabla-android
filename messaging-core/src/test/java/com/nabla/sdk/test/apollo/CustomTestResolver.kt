package com.nabla.sdk.test.apollo

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.test.DefaultTestResolver
import com.benasher44.uuid.uuid4
import com.nabla.sdk.graphql.type.DateTime
import com.nabla.sdk.graphql.type.UUID
import kotlinx.datetime.Clock

@OptIn(ApolloExperimental::class)
/**
 * A wrapper around DefaultTestResolver that can generate custom scalar values.
 * See https://www.apollographql.com/docs/kotlin/essentials/custom-scalars
 */
internal class CustomTestResolver : DefaultTestResolver() {
    override fun <T> resolve(
        responseName: String,
        compiledType: CompiledType,
        enumValues: List<String>,
        ctors: Array<out () -> Map<String, Any?>>?
    ): T {
        val customValue = when (compiledType.leafType().name) {
            UUID.type.name -> uuid4().toString()
            DateTime.type.name -> Clock.System.now().toString()
            else -> null
        }
        return if (customValue != null) {
            @Suppress("UNCHECKED_CAST")
            customValue as T
        } else {
            super.resolve(responseName, compiledType, enumValues, ctors)
        }
    }
}
