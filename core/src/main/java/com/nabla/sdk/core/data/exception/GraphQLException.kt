package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.exception.ApolloException
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public class GraphQLException internal constructor(
    internal val error: com.apollographql.apollo3.api.Error,
    internal val requestId: String?,
) : ApolloException(error.message) {
    public val numericCode: Int? = (error.extensions?.get("errorCode") as? Int)
    internal val serverMessage: String = error.message
}
