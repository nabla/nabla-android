package com.nabla.sdk.scheduling

import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal interface SchedulingInternalModule : SchedulingModule {
    suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>>
    suspend fun getAppointmentLocations(): Result<Set<AppointmentLocation>>

    fun watchAvailabilitySlots(
        categoryId: CategoryId,
    ): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>>

    fun watchPastAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>
    fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>

    suspend fun getAppointmentConfirmationConsents(appointmentLocation: AppointmentLocation): Result<AppointmentConfirmationConsents>

    suspend fun scheduleAppointment(
        location: AppointmentLocation,
        categoryId: CategoryId,
        providerId: UUID,
        slot: Instant,
    ): Result<Appointment>

    suspend fun cancelAppointment(id: AppointmentId): Result<Unit>
}
