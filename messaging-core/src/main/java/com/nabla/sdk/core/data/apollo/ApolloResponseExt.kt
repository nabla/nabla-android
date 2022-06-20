package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.network.http.HttpInfo
import com.nabla.sdk.core.data.exception.GraphQLException

private const val REQUEST_ID_HEADER_NAME = "x-request-id"

internal val <D : Operation.Data> ApolloResponse<D>.dataOrThrowOnError: D
    get(): D {
        val requestId = (executionContext as? HttpInfo)?.headers?.firstOrNull { it.name == REQUEST_ID_HEADER_NAME }?.value
        val error = errors?.firstOrNull()
        if (error != null) {
            throw GraphQLException(error, requestId)
        }

        return data ?: throw ApolloNoDataException(requestId)
    }
