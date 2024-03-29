package com.nabla.sdk.messaging.core.data.conversation

import app.cash.turbine.test
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.apollo.CoreGqlMapper
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.stubs.TestClock
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.stubs.GqlData
import com.nabla.sdk.messaging.graphql.ConversationsEventsSubscription
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.apollo.FlowTestNetworkTransport
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
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class GqlConversationDataSourceTest : BaseCoroutineTest() {

    @Test
    fun `created conversation event is notified to conversations watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationDataSource) = setupTest(this)

        testNetworkTransport.register(
            GqlConversationDataSource.conversationsQuery(),
            GqlData.Conversations.empty(),
        )
        val conversationsEventsSubscriptionResponseFlow =
            MutableStateFlow<ConversationsEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsEventsSubscriptionResponseFlow.filterNotNull(),
        )

        // It was flacky on CI so the timeout is a little bigger than default here (1s by default)
        gqlConversationDataSource.watchConversations().test(timeout = 5.seconds) {
            var paginatedConversationsResponse = awaitItem()
            assertTrue(paginatedConversationsResponse.data.items.isEmpty(), "item size should be 0")
            conversationsEventsSubscriptionResponseFlow.value =
                GqlData.ConversationsEvents.conversationCreated()
            paginatedConversationsResponse = awaitItem()
            assertTrue(paginatedConversationsResponse.data.items.size == 1, "item size should be one")
        }
        job.cancel()
    }

    @Test
    @ApolloExperimental
    fun `load more conversation is notified to conversations watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationDataSource) = setupTest(this)

        val cursor = uuid4().toString()
        testNetworkTransport.register(
            GqlConversationDataSource.conversationsQuery(),
            GqlData.Conversations.single {
                nextCursor = cursor
                hasMore = true
            },
        )
        testNetworkTransport.register(
            GqlConversationDataSource.conversationsQuery(cursor),
            GqlData.Conversations.single {
                nextCursor = null
                hasMore = false
            },
        )
        testNetworkTransport.register(
            ConversationsEventsSubscription(),
            emptyFlow(),
        )
        gqlConversationDataSource.watchConversations().test {
            var paginatedConversationsResponse = awaitItem()
            assertTrue(paginatedConversationsResponse.data.items.size == 1)
            launch { gqlConversationDataSource.loadMoreConversationsInCache() }
            paginatedConversationsResponse = awaitItem()
            assertTrue(paginatedConversationsResponse.data.items.size == 2)
        }
        job.cancel()
    }

    private fun setupTest(testScope: TestScope): Triple<FlowTestNetworkTransport, CompletableJob, GqlConversationDataSource> {
        val testNetworkTransport = FlowTestNetworkTransport()
        val job = Job()
        val gqlConversationDataSource =
            createTestableGqlConversationDataSource(testNetworkTransport, testScope + job, testScope)
        return Triple(testNetworkTransport, job, gqlConversationDataSource)
    }

    private fun createTestableGqlConversationDataSource(
        testNetworkTransport: NetworkTransport,
        scope: CoroutineScope,
        testScope: TestScope,
    ): GqlConversationDataSource {
        val apolloClient = ApolloFactory.configureBuilder(
            normalizedCacheFactory = MemoryCacheFactory(),
        ).networkTransport(testNetworkTransport)
            .build()
        val logger: Logger = mockk(relaxed = true)
        return GqlConversationDataSource(
            logger = logger,
            coroutineScope = scope,
            apolloClient = apolloClient,
            mapper = GqlMapper(logger, LocalConversationDataSource(), CoreGqlMapper(logger)),
            exceptionMapper = NablaExceptionMapper(),
            clock = TestClock(testScope),
        )
    }
}
