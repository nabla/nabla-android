package com.nabla.sdk.scheduling.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.scheduling.data.apollo.GqlMapper
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.graphql.AppointmentAvailableLocationsQuery

internal class GqlAppointmentLocationDataSource(
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {
    suspend fun getAvailableLocations(): Set<AppointmentLocation> {
        return apolloClient.query(AppointmentAvailableLocationsQuery())
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .execute()
            .dataOrThrowOnError
            .appointmentAvailableLocations.let { gqlData ->
                mutableSetOf<AppointmentLocation>().apply {
                    if (gqlData.hasRemoteAvailabilities) add(AppointmentLocation.REMOTE)
                    if (gqlData.hasPhysicalAvailabilities) add(AppointmentLocation.PHYSICAL)
                }.toSet()
            }
    }
}
