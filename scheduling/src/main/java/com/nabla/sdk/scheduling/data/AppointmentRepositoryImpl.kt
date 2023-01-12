package com.nabla.sdk.scheduling.data

import com.nabla.sdk.core.data.exception.GraphQLException
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.scheduling.domain.boundary.AppointmentRepository
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal class AppointmentRepositoryImpl(
    private val repoScope: CoroutineScope,
    private val gqlAppointmentDataSource: GqlAppointmentDataSource,
    private val gqlAppointmentCategoryDataSource: GqlAppointmentCategoryDataSource,
    private val gqlAppointmentConfirmConsentsDataSource: GqlAppointmentConfirmConsentsDataSource,
    private val gqlAppointmentLocationDataSource: GqlAppointmentLocationDataSource,
) : AppointmentRepository {

    override fun watchUpcomingAppointments(): Flow<PaginatedList<Appointment>> = gqlAppointmentDataSource.watchUpcomingAppointments()
    override fun watchPastAppointments(): Flow<PaginatedList<Appointment>> = gqlAppointmentDataSource.watchPastAppointments()

    private val loadMoreUpcomingAppointmentsSharedSingle: SharedSingle<Unit, Result<Unit>> =
        sharedSingleIn(repoScope) { gqlAppointmentDataSource.loadMoreUpcomingAppointments() }
    private val loadMorePastAppointmentsSharedSingle: SharedSingle<Unit, Result<Unit>> =
        sharedSingleIn(repoScope) { gqlAppointmentDataSource.loadMorePastAppointments() }

    override suspend fun loadMoreUpcomingAppointments() = loadMoreUpcomingAppointmentsSharedSingle.await()
    override suspend fun loadMorePastAppointments() = loadMorePastAppointmentsSharedSingle.await()

    override suspend fun getCategories(): List<AppointmentCategory> = gqlAppointmentCategoryDataSource.getCategories()
    override suspend fun getLocations(): Set<AppointmentLocation> {
        return gqlAppointmentLocationDataSource.getAvailableLocations()
    }

    override fun watchAvailabilitySlots(categoryId: CategoryId): Flow<PaginatedList<AvailabilitySlot>> =
        gqlAppointmentCategoryDataSource.watchAvailabilitySlots(categoryId)

    private val loadMoreAvailabilitySlotsSharedSingleMap = mutableMapOf<CategoryId, SharedSingle<Unit, Result<Unit>>>()
    override suspend fun loadMoreAvailabilitySlots(categoryId: CategoryId) = loadMoreAvailabilitySlotsSharedSingleMap
        .getOrPut(categoryId) {
            sharedSingleIn(repoScope) {
                gqlAppointmentCategoryDataSource.loadMoreAvailabilitySlots(categoryId)
            }
        }
        .await()

    override suspend fun getAppointmentConfirmationConsents(
        appointmentLocation: AppointmentLocation
    ): AppointmentConfirmationConsents {
        return gqlAppointmentConfirmConsentsDataSource.getConfirmConsents(appointmentLocation)
    }

    override suspend fun scheduleAppointment(
        location: AppointmentLocation,
        categoryId: CategoryId,
        providerId: UUID,
        slot: Instant,
    ): Appointment {
        return try {
            gqlAppointmentDataSource.scheduleAppointment(location, categoryId, providerId, slot)
        } catch (gqlException: GraphQLException) {
            try {
                // very likely the input is not valid anymore (e.g. the selected slot is not available anymore)
                gqlAppointmentCategoryDataSource.resetAvailabilitySlotsCache(categoryId)
            } catch (e: Exception) {
                throw e.initCause(gqlException)
            }

            throw gqlException
        }
    }

    override suspend fun cancelAppointment(id: AppointmentId) {
        return gqlAppointmentDataSource.cancelAppointment(id)
    }
}
