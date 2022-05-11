package com.nabla.sdk.messaging.core.data.conversation

import app.cash.turbine.test
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.network.NetworkTransport
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.apollo.test.CustomTestResolver
import com.nabla.sdk.core.data.apollo.test.FlowTestNetworkTransport
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.graphql.test.ConversationListQuery_TestBuilder.Data
import com.nabla.sdk.graphql.test.ConversationsEventsSubscription_TestBuilder.Data
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
internal class GqlConversationDataSourceTest {

    @Test
    fun `created conversation event is notified to conversations watcher`() = runTest {
        val testNetworkTransport = FlowTestNetworkTransport()
        val job = Job()
        val gqlConversationDataSource = createTestableGqlConversationDataSource(
            testNetworkTransport,
            this + job
        )

        testNetworkTransport.register(
            GqlConversationDataSource.FIRST_CONVERSATIONS_PAGE_QUERY,
            ConversationListQuery.Data {
                conversations = conversations {
                    conversations = emptyList()
                }
            }
        )
        val conversationsEventsSubscriptionResponseFlow =
            MutableStateFlow<ConversationsEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsEventsSubscriptionResponseFlow.filterNotNull()
        )

        gqlConversationDataSource.watchConversations().test {
            var paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.items.isEmpty())
            conversationsEventsSubscriptionResponseFlow.value =
                ConversationsEventsSubscription.Data(CustomTestResolver()) {
                    conversations = conversations {
                        event = conversationCreatedEventEvent { }
                    }
                }
            paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.items.size == 1)
        }
        job.cancel()
    }

    private fun createTestableGqlConversationDataSource(
        testNetworkTransport: NetworkTransport,
        scope: CoroutineScope
    ): GqlConversationDataSource {
        val apolloClient = ApolloFactory.configureBuilder(
            normalizedCacheFactory = MemoryCacheFactory()
        ).networkTransport(testNetworkTransport)
            .build()
        val logger: Logger = mockk(relaxed = true)
        return GqlConversationDataSource(
            logger = logger,
            coroutineScope = scope,
            apolloClient = apolloClient,
            mapper = GqlMapper(logger)
        )
    }
}
