package com.nabla.sdk.scheduling.domain.boundary

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal interface AppointmentRepository {
    fun watchPastAppointments(): Flow<PaginatedList<Appointment>>
    fun watchUpcomingAppointments(): Flow<PaginatedList<Appointment>>
    suspend fun loadMorePastAppointments()
    suspend fun loadMoreUpcomingAppointments()

    suspend fun getCategories(): List<AppointmentCategory>
    suspend fun getLocationTypes(): Set<AppointmentLocationType>

    fun watchAvailabilitySlots(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId
    ): Flow<PaginatedList<AvailabilitySlot>>
    suspend fun loadMoreAvailabilitySlots(locationType: AppointmentLocationType, categoryId: AppointmentCategoryId)

    suspend fun getAppointmentConfirmationConsents(locationType: AppointmentLocationType): AppointmentConfirmationConsents

    suspend fun scheduleAppointment(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant,
    ): Appointment

    suspend fun cancelAppointment(id: AppointmentId)

    suspend fun getAppointment(id: AppointmentId): Appointment
}
