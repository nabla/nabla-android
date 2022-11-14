package com.nabla.sdk.scheduling.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.scheduling.data.apollo.GqlMapper
import com.nabla.sdk.scheduling.graphql.AppointmentConfirmationConsentsQuery

internal class GqlAppointmentConfirmConsentsDataSource(
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {
    suspend fun getConfirmConsents() = apolloClient.query(AppointmentConfirmationConsentsQuery())
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .execute()
        .dataOrThrowOnError
        .appointmentConfirmationConsents
        .let { mapper.mapToAppointmentConfirmationConsents(it) }
}
