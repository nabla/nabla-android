package com.nabla.sdk.core.data.patient

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.ApolloResponseExt.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.CoreGqlMapper
import com.nabla.sdk.core.domain.boundary.ProviderRepository
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.graphql.ProviderQuery

internal class ProviderRepositoryImpl(
    private val apolloClient: ApolloClient,
    private val coreGqlMapper: CoreGqlMapper,
) : ProviderRepository {
    override suspend fun getProvider(id: Uuid): Provider {
        return apolloClient.query(ProviderQuery(id))
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
            .dataOrThrowOnError
            .provider
            .provider
            .providerFragment
            .let(coreGqlMapper::mapToProvider)
    }
}
