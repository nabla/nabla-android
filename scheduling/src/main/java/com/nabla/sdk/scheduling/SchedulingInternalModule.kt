package com.nabla.sdk.scheduling

import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
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

internal interface SchedulingInternalModule : SchedulingModule {
    suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>>
    suspend fun getAppointmentLocationTypes(): Result<Set<AppointmentLocationType>>

    fun watchAvailabilitySlots(
        categoryId: AppointmentCategoryId,
    ): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>>

    fun watchPastAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>
    fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>

    suspend fun getAppointmentConfirmationConsents(
        locationType: AppointmentLocationType
    ): Result<AppointmentConfirmationConsents>

    suspend fun scheduleAppointment(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant,
    ): Result<Appointment>

    suspend fun cancelAppointment(id: AppointmentId): Result<Unit>

    suspend fun getAppointment(id: AppointmentId): Result<Appointment>
}
