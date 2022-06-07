package com.nabla.sdk.messaging.core.data.message

import app.cash.turbine.test
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationItemsQuery
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.stubs.GqlData
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import com.nabla.sdk.test.apollo.FlowTestNetworkTransport
import io.mockk.mockk
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GqlConversationContentDataSourceTest {

    @Test
    fun `created message event is notified to conversations watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.empty(conversationId)
        )
        val gqlEventEmitter = MutableStateFlow<ConversationEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            gqlEventEmitter.filterNotNull()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.conversationItems.items.isEmpty())
            gqlEventEmitter.value =
                GqlData.ConversationEvents.MessageCreated.patientTextMessage(conversationId)
            paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.conversationItems.items.size == 1)
        }
        job.cancel()
    }

    @Test
    fun `load more message is notified to conversation watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        val cursor = uuid4().toString()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = cursor
                hasMore = true
            }
        )
        testNetworkTransport.register(
            ConversationItemsQuery(
                conversationId.value,
                OpaqueCursorPage(cursor = Optional.Present(cursor))
            ),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = null
                hasMore = false
            }
        )
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            emptyFlow()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedItems = awaitItem()
            assertTrue(paginatedItems.conversationItems.items.size == 1)
            launch {
                gqlConversationContentDataSource.loadMoreConversationItemsInCache(
                    conversationId
                )
            }
            paginatedItems = awaitItem()
            assertTrue(paginatedItems.conversationItems.items.size == 2)
        }
        job.cancel()
    }

    private fun setupTest(testScope: TestScope): Triple<FlowTestNetworkTransport, CompletableJob, GqlConversationContentDataSource> {
        val testNetworkTransport = FlowTestNetworkTransport()
        val job = Job()
        val gqlConversationContentDataSource =
            createTestableGqlMessageDataSource(testNetworkTransport, testScope + job)
        return Triple(testNetworkTransport, job, gqlConversationContentDataSource)
    }

    private fun createTestableGqlMessageDataSource(
        testNetworkTransport: NetworkTransport,
        scope: CoroutineScope
    ): GqlConversationContentDataSource {
        val apolloClient = ApolloFactory.configureBuilder(
            normalizedCacheFactory = MemoryCacheFactory()
        ).networkTransport(testNetworkTransport)
            .build()
        val logger: Logger = mockk(relaxed = true)
        return GqlConversationContentDataSource(
            logger = logger,
            coroutineScope = scope,
            apolloClient = apolloClient,
            mapper = GqlMapper(logger)
        )
    }
}
