package com.nabla.sdk.core.data.patient

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SessionLocalDataCleanerImpl(
    private val apolloClient: ApolloClient,
    private val localPatientDataSource: LocalPatientDataSource,
    private val tokenLocalDataSource: TokenLocalDataSource,
) : SessionLocalDataCleaner {
    override suspend fun cleanLocalSessionData() {
        localPatientDataSource.setPatient(null)
        tokenLocalDataSource.clear()
        withContext(Dispatchers.IO) {
            apolloClient.apolloStore.clearAll()
        }
    }
}
