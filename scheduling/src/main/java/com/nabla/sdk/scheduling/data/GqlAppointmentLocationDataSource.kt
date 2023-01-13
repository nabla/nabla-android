package com.nabla.sdk.scheduling.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.graphql.AppointmentAvailableLocationsQuery

internal class GqlAppointmentLocationDataSource(
    private val apolloClient: ApolloClient,
) {
    suspend fun getAvailableLocationTypes(): Set<AppointmentLocationType> {
        return apolloClient.query(AppointmentAvailableLocationsQuery())
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .execute()
            .dataOrThrowOnError
            .appointmentAvailableLocations.let { gqlData ->
                mutableSetOf<AppointmentLocationType>().apply {
                    if (gqlData.hasRemoteAvailabilities) add(AppointmentLocationType.REMOTE)
                    if (gqlData.hasPhysicalAvailabilities) add(AppointmentLocationType.PHYSICAL)
                }.toSet()
            }
    }
}
