package com.nabla.sdk.core.data.patient

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner

internal class SessionLocalDataCleanerImpl(
    private val apolloClient: ApolloClient,
    private val localPatientDataSource: LocalPatientDataSource,
    private val tokenLocalDataSource: TokenLocalDataSource,
) : SessionLocalDataCleaner {
    override fun cleanLocalSessionData() {
        localPatientDataSource.setPatient(null)
        tokenLocalDataSource.setAccessToken(null)
        tokenLocalDataSource.setRefreshToken(null)
        apolloClient.apolloStore.clearAll()
    }
}
