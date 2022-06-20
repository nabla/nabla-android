package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.exception.ApolloException

internal class GraphQLException(
    val error: com.apollographql.apollo3.api.Error,
    val requestId: String?,
) : ApolloException(error.message) {
    val numericCode: Int? = (error.extensions?.get("errorCode") as? Int)
    val serverMessage: String = error.message
}
