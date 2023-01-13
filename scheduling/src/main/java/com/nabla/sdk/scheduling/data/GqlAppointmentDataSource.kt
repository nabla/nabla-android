package com.nabla.sdk.scheduling.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.GQL_DOMAIN
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.type.AppointmentsPage
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.scheduling.data.apollo.GqlMapper
import com.nabla.sdk.scheduling.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.graphql.AppointmentQuery
import com.nabla.sdk.scheduling.graphql.AppointmentsEventsSubscription
import com.nabla.sdk.scheduling.graphql.CancelAppointmentMutation
import com.nabla.sdk.scheduling.graphql.PastAppointmentsQuery
import com.nabla.sdk.scheduling.graphql.ScheduleAppointmentMutation
import com.nabla.sdk.scheduling.graphql.UpcomingAppointmentsQuery
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentFragment
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentsPageFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.util.UUID

internal class GqlAppointmentDataSource(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {
    private val appointmentsEventsFlow by lazy {
        apolloClient.subscription(AppointmentsEventsSubscription())
            .toFlow()
            .retryOnNetworkErrorAndShareIn(coroutineScope).onEach { response ->
                val event = response.dataOrThrowOnError.appointments?.event
                logger.debug(domain = GQL_DOMAIN, message = "Event $event")
                event?.onAppointmentCreatedEvent?.appointment?.appointmentFragment?.let {
                    handleNewAppointment(it)
                }
                event?.onAppointmentCancelledEvent?.appointmentId?.let {
                    handleCancelledAppointment(it)
                }
                event?.onAppointmentUpdatedEvent?.appointment?.appointmentFragment?.let {
                    handleUpdatedAppointment(it)
                }
            }
    }

    suspend fun getAppointment(appointmentId: AppointmentId): Appointment {
        return apolloClient.query(AppointmentQuery(appointmentId = appointmentId.uuid))
            .execute()
            .dataOrThrowOnError
            .let { mapper.mapToAppointment(it.appointment.appointment.appointmentFragment) }
    }

    suspend fun scheduleAppointment(
        location: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant,
    ): Appointment {
        return apolloClient.mutation(
            ScheduleAppointmentMutation(
                categoryId = categoryId.value,
                providerId = providerId,
                isPhysical = location == AppointmentLocationType.PHYSICAL,
                slot = slot,
                timezone = TimeZone.currentSystemDefault()
            )
        ).execute()
            .dataOrThrowOnError
            .scheduleAppointmentV2.appointment.appointmentFragment
            .also { appointmentFragment ->
                updateUpcomingAppointmentsCache(inserterOf(appointmentFragment))
            }
            .let(mapper::mapToAppointment)
    }

    suspend fun cancelAppointment(id: AppointmentId) {
        apolloClient.mutation(CancelAppointmentMutation(id.uuid))
            .execute()
            .dataOrThrowOnError
            .apply {
                handleCancelledAppointment(cancelAppointment.appointmentUuid)
            }
    }

    fun watchUpcomingAppointments(): Flow<PaginatedList<Appointment>> {
        val dataFlow = apolloClient.query(upcomingAppointmentsQuery())
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true)
            .map { response -> response.dataOrThrowOnError }
            .map { queryData ->
                val items = queryData.upcomingAppointments.appointmentsPageFragment.data.map {
                    mapper.mapToAppointment(it.appointmentFragment)
                }.sortedBy { appointment -> appointment.scheduledAt }
                    .filter { it.state == AppointmentState.UPCOMING }

                PaginatedList(items, queryData.upcomingAppointments.appointmentsPageFragment.hasMore)
            }

        return flowOf(appointmentsEventsFlow, dataFlow)
            .flattenMerge()
            .filterIsInstance()
    }

    fun watchPastAppointments(): Flow<PaginatedList<Appointment>> {
        val dataFlow = apolloClient.query(pastAppointmentsQuery())
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true)
            .map { response -> response.dataOrThrowOnError }
            .map { queryData ->
                val page = queryData.pastAppointments.appointmentsPageFragment
                val items = page.data.map {
                    mapper.mapToAppointment(it.appointmentFragment)
                }.sortedByDescending { appointment -> appointment.scheduledAt }
                    .filter { it.state == AppointmentState.FINALIZED }

                PaginatedList(items, page.hasMore)
            }

        return flowOf(appointmentsEventsFlow, dataFlow)
            .flattenMerge()
            .filterIsInstance()
    }

    suspend fun loadMoreUpcomingAppointments() {
        updateUpcomingAppointmentsCache { cachedPages ->
            if (!cachedPages.hasMore) null else {
                val updatedQuery = upcomingAppointmentsQuery(cachedPages.nextCursor)

                val freshQueryData = apolloClient.query(updatedQuery)
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
                    .dataOrThrowOnError
                val mergedAppointments =
                    (cachedPages.data + freshQueryData.upcomingAppointments.appointmentsPageFragment.data)
                        .distinctBy { it.appointmentFragment.id }

                freshQueryData.upcomingAppointments.appointmentsPageFragment.copy(data = mergedAppointments)
            }
        }
    }

    suspend fun loadMorePastAppointments() {
        updatePastAppointmentsCache { cachedPages ->
            if (!cachedPages.hasMore) null else {
                val updatedQuery = pastAppointmentsQuery(cachedPages.nextCursor)

                val freshQueryData = apolloClient.query(updatedQuery)
                    .fetchPolicy(FetchPolicy.NetworkOnly)
                    .execute()
                    .dataOrThrowOnError
                val mergedAppointments =
                    (cachedPages.data + freshQueryData.pastAppointments.appointmentsPageFragment.data)
                        .distinctBy { it.appointmentFragment.id }

                freshQueryData.pastAppointments.appointmentsPageFragment.copy(data = mergedAppointments)
            }
        }
    }

    private suspend fun handleNewAppointment(appointment: AppointmentFragment) {
        val inserter = inserterOf(appointment)

        if (appointment.state.onFinalizedAppointment != null) {
            updatePastAppointmentsCache(inserter)
        } else {
            updateUpcomingAppointmentsCache(inserter)
        }
    }

    private suspend fun handleCancelledAppointment(appointmentId: Uuid) {
        val deleter = deleterOf(appointmentId)

        updateUpcomingAppointmentsCache(deleter)
        updatePastAppointmentsCache(deleter)
    }

    private suspend fun handleUpdatedAppointment(appointment: AppointmentFragment) {
        val deleter = deleterOf(appointment.id)
        val inserter = inserterOf(appointment)

        // we only have to handle this event by hand if it's a "upcoming to finalized" state change, e.g. moving it from one list to the other
        if (appointment.state.onFinalizedAppointment != null) {
            updateUpcomingAppointmentsCache(deleter)
            updatePastAppointmentsCache(inserter)
        }
    }

    private fun inserterOf(appointment: AppointmentFragment) = { cachedPages: AppointmentsPageFragment ->
        cachedPages
            .takeIf { cachedPages.data.none { it.appointmentFragment.id == appointment.id } }
            ?.copy(
                data = cachedPages.data
                    .plus(AppointmentsPageFragment.Data(AppointmentsPage.type.name, appointment))
                    .sortedBy { it.appointmentFragment.scheduledAt }
                    .distinctBy { it.appointmentFragment.id }
            )
    }

    private fun deleterOf(appointmentId: Uuid) = { cachedPages: AppointmentsPageFragment ->
        cachedPages
            .takeIf { cachedPages.data.any { it.appointmentFragment.id == appointmentId } }
            ?.copy(
                data = cachedPages.data
                    .filter { it.appointmentFragment.id != appointmentId }
            )
    }

    private suspend fun updateUpcomingAppointmentsCache(
        updater: suspend (cachedPages: AppointmentsPageFragment) -> AppointmentsPageFragment?,
    ) = apolloClient.updateCache(upcomingAppointmentsQuery()) { cachedQueryData ->
        val cachedPages = cachedQueryData?.upcomingAppointments?.appointmentsPageFragment
            ?: return@updateCache CacheUpdateOperation.Ignore()

        val mergedAppointments = updater(cachedPages) ?: return@updateCache CacheUpdateOperation.Ignore()
        val mergedQueryData = cachedQueryData.modify(mergedAppointments)

        CacheUpdateOperation.Write(mergedQueryData)
    }

    private suspend fun updatePastAppointmentsCache(
        updater: suspend (cachedPages: AppointmentsPageFragment) -> AppointmentsPageFragment?,
    ) = apolloClient.updateCache(pastAppointmentsQuery()) { cachedQueryData ->
        val cachedPages = cachedQueryData?.pastAppointments?.appointmentsPageFragment
            ?: return@updateCache CacheUpdateOperation.Ignore()

        val mergedAppointments = updater(cachedPages) ?: return@updateCache CacheUpdateOperation.Ignore()
        val mergedQueryData = cachedQueryData.modify(mergedAppointments)

        CacheUpdateOperation.Write(mergedQueryData)
    }

    companion object {
        private fun upcomingAppointmentsQuery(cursorPage: String? = null) = UpcomingAppointmentsQuery(
            OpaqueCursorPage(cursor = Optional.presentIfNotNull(cursorPage), numberOfItems = Optional.Present(50))
        )
        private fun pastAppointmentsQuery(cursorPage: String? = null) = PastAppointmentsQuery(
            OpaqueCursorPage(cursor = Optional.presentIfNotNull(cursorPage), numberOfItems = Optional.Present(50))
        )
    }
}
