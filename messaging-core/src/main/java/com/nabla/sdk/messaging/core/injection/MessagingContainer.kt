package com.nabla.sdk.messaging.core.injection

import com.apollographql.apollo3.ApolloClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.messaging.core.GqlMessageDataSource
import com.nabla.sdk.messaging.core.data.ConversationRepositoryImpl
import com.nabla.sdk.messaging.core.data.LocalMessageDataSource
import com.nabla.sdk.messaging.core.data.MessageRepositoryImpl
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlEventHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlMapper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class MessagingContainer(logger: Logger, apolloClient: ApolloClient) {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messagingGqlMapper = MessagingGqlMapper()
    private val messagingGqlOperationHelper = MessagingGqlOperationHelper(apolloClient)
    private val localMessageDataSource = LocalMessageDataSource()
    private val messagingGqlEventHelper = MessagingGqlEventHelper(
        apolloClient,
        repoScope,
        messagingGqlOperationHelper,
        localMessageDataSource,
    )
    private val gqlMessageDataSource = GqlMessageDataSource(
        apolloClient = apolloClient,
        gqlEventHelper = messagingGqlEventHelper,
        mapper = messagingGqlMapper,
    )

    private val conversationRepositoryImpl = ConversationRepositoryImpl(
        logger = logger,
        repoScope = repoScope,
        apolloClient = apolloClient,
        mapper = messagingGqlMapper,
        gqlEventHelper = messagingGqlEventHelper,
        gqlOperationHelper = messagingGqlOperationHelper
    )

    private val messageRepositoryImpl = MessageRepositoryImpl(
        repoScope = repoScope,
        gqlOperationHelper = messagingGqlOperationHelper,
        localMessageDataSource = localMessageDataSource,
        gqlMessageDataSource = gqlMessageDataSource,
    )

    val conversationRepository: ConversationRepository = conversationRepositoryImpl
    val messageRepository: MessageRepository = messageRepositoryImpl
}
