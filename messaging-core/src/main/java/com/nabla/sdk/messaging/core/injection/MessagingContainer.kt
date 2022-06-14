package com.nabla.sdk.messaging.core.injection

import com.apollographql.apollo3.ApolloClient
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.conversation.ConversationRepositoryImpl
import com.nabla.sdk.messaging.core.data.conversation.GqlConversationDataSource
import com.nabla.sdk.messaging.core.data.message.ConversationContentRepositoryImpl
import com.nabla.sdk.messaging.core.data.message.GqlConversationContentDataSource
import com.nabla.sdk.messaging.core.data.message.LocalMessageDataSource
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock

internal class MessagingContainer(
    logger: Logger,
    apolloClient: ApolloClient,
    fileUploadRepository: FileUploadRepository,
    val nablaExceptionMapper: NablaExceptionMapper,
    val sessionClient: SessionClient,
    clock: Clock,
    uuidGenerator: UuidGenerator,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gqlMapper = GqlMapper(logger)
    private val localMessageDataSource = LocalMessageDataSource()
    private val gqlConversationContentDataSource = GqlConversationContentDataSource(
        logger = logger,
        apolloClient = apolloClient,
        mapper = gqlMapper,
        coroutineScope = repoScope
    )
    private val gqlConversationDataSource = GqlConversationDataSource(
        logger = logger,
        coroutineScope = repoScope,
        apolloClient = apolloClient,
        mapper = gqlMapper,
        clock = clock,
    )

    private val conversationRepositoryImpl = ConversationRepositoryImpl(
        repoScope = repoScope,
        gqlConversationDataSource = gqlConversationDataSource,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
    )

    private val conversationContentRepositoryImpl = ConversationContentRepositoryImpl(
        repoScope = repoScope,
        localMessageDataSource = localMessageDataSource,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
        fileUploadRepository = fileUploadRepository,
        clock = clock,
        uuidGenerator = uuidGenerator,
        logger = logger,
    )

    val conversationRepository: ConversationRepository = conversationRepositoryImpl
    val conversationContentRepository: ConversationContentRepository = conversationContentRepositoryImpl
}
