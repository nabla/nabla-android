package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.exception.ApolloException
import okhttp3.Response
import java.math.BigDecimal

internal class GraphQLException(
    val error: com.apollographql.apollo3.api.Error,
    context: Response,
) : ApolloException(error.message) {
    private val extensions = error.nonStandardFields?.get("extensions") as? Map<*, *>

    val numericCode: Int? = (extensions?.get("errorCode") as? BigDecimal)?.toInt()
    val serverMessage: String = error.message

    // not always present, but if so it's useful debugging information
    val optionalDetailedMessage: String? = extensions?.get("detailedMessage") as? String

    val requestId: String? = (context.networkResponse)?.header(REQUEST_ID_HEADER_NAME)

    companion object {
        private const val REQUEST_ID_HEADER_NAME = "x-request-id"
    }
}
