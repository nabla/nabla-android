package com.nabla.sdk.core.data.patient

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.nabla.sdk.core.data.auth.TokenLocalDataSource
import com.nabla.sdk.core.domain.boundary.LogoutInteractor
import com.nabla.sdk.core.domain.entity.AuthenticationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LogoutInteractorImpl(
    private val apolloClient: ApolloClient,
    private val localPatientDataSource: LocalPatientDataSource,
    private val tokenLocalDataSource: TokenLocalDataSource,
) : LogoutInteractor {

    override suspend fun logout() {
        clearLocalSessionData()
        closeWebsocketConnection()
    }

    private suspend fun clearLocalSessionData() {
        localPatientDataSource.setPatient(null)
        tokenLocalDataSource.clear()
        withContext(Dispatchers.IO) {
            apolloClient.apolloStore.clearAll()
        }
    }

    private fun closeWebsocketConnection() {
        (apolloClient.subscriptionNetworkTransport as? WebSocketNetworkTransport)?.closeConnection(
            reason = AuthenticationException.UserIdNotSet(),
        )
    }
}
