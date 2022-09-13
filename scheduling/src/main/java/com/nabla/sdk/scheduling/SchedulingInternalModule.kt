package com.nabla.sdk.scheduling

import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal interface SchedulingInternalModule {
    suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>>

    fun watchAvailabilitySlots(
        categoryId: CategoryId,
    ): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>>

    fun watchPastAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>
    fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>>

    suspend fun scheduleAppointment(
        categoryId: CategoryId,
        providerId: UUID,
        slot: Instant,
    ): Result<Appointment>

    suspend fun cancelAppointment(id: AppointmentId): Result<Unit>
}
