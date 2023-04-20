package com.nabla.sdk.messaging.core.injection

import com.apollographql.apollo3.ApolloClient
import com.nabla.sdk.core.data.apollo.CoreGqlMapper
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.conversation.ConversationRepositoryImpl
import com.nabla.sdk.messaging.core.data.conversation.GqlConversationDataSource
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.data.message.ConversationContentRepositoryImpl
import com.nabla.sdk.messaging.core.data.message.GqlConversationContentDataSource
import com.nabla.sdk.messaging.core.data.message.LocalMessageDataSource
import com.nabla.sdk.messaging.core.data.message.MessageFileUploader
import com.nabla.sdk.messaging.core.data.message.MessageMapper
import com.nabla.sdk.messaging.core.data.message.SendMessageOrchestrator
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

internal class MessagingContainer(
    logger: Logger,
    coreGqlMapper: CoreGqlMapper,
    apolloClient: ApolloClient,
    fileUploadRepository: FileUploadRepository,
    val nablaExceptionMapper: NablaExceptionMapper,
    val sessionClient: SessionClient,
    clock: Clock,
    uuidGenerator: UuidGenerator,
    maybeVideoCallModule: VideoCallModule?,
    val eventsConnectionStateFlow: Flow<EventsConnectionState>,
    backgroundScope: CoroutineScope,
) {
    private val localConversationDataSource = LocalConversationDataSource()
    private val gqlMapper = GqlMapper(logger, localConversationDataSource, coreGqlMapper)
    private val localMessageDataSource = LocalMessageDataSource()
    private val messageFileUploader = MessageFileUploader(fileUploadRepository)
    private val gqlConversationContentDataSource = GqlConversationContentDataSource(
        logger = logger,
        apolloClient = apolloClient,
        mapper = gqlMapper,
        exceptionMapper = nablaExceptionMapper,
        coroutineScope = backgroundScope,
        localConversationDataSource = localConversationDataSource,
    )
    private val gqlConversationDataSource = GqlConversationDataSource(
        logger = logger,
        coroutineScope = backgroundScope,
        apolloClient = apolloClient,
        mapper = gqlMapper,
        exceptionMapper = nablaExceptionMapper,
        clock = clock,
    )
    private val messageMapper = MessageMapper(
        clock = clock,
        uuidGenerator = uuidGenerator,
        logger = logger,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
    )
    private val sendMessageOrchestrator = SendMessageOrchestrator(
        localConversationDataSource = localConversationDataSource,
        localMessageDataSource = localMessageDataSource,
        gqlConversationDataSource = gqlConversationDataSource,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
        messageMapper = messageMapper,
        messageFileUploader = messageFileUploader,
    )

    private val conversationRepositoryImpl = ConversationRepositoryImpl(
        repoScope = backgroundScope,
        localConversationDataSource = localConversationDataSource,
        gqlConversationDataSource = gqlConversationDataSource,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
        messageMapper = messageMapper,
        messageFileUploader = messageFileUploader,
    )

    private val conversationContentRepositoryImpl = ConversationContentRepositoryImpl(
        repoScope = backgroundScope,
        localMessageDataSource = localMessageDataSource,
        localConversationDataSource = localConversationDataSource,
        gqlConversationContentDataSource = gqlConversationContentDataSource,
        sendMessageOrchestrator = sendMessageOrchestrator,
        messageMapper = messageMapper,
        isVideoCallModuleActive = maybeVideoCallModule != null,
    )

    val conversationRepository: ConversationRepository = conversationRepositoryImpl
    val conversationContentRepository: ConversationContentRepository = conversationContentRepositoryImpl
}
