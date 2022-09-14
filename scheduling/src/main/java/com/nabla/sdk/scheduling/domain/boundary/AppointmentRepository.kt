package com.nabla.sdk.scheduling.domain.boundary

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal interface AppointmentRepository {
    fun watchPastAppointments(): Flow<PaginatedList<Appointment>>
    fun watchUpcomingAppointments(): Flow<PaginatedList<Appointment>>
    suspend fun loadMorePastAppointments()
    suspend fun loadMoreUpcomingAppointments()

    suspend fun getCategories(): List<AppointmentCategory>

    fun watchAvailabilitySlots(categoryId: CategoryId): Flow<PaginatedList<AvailabilitySlot>>
    suspend fun loadMoreAvailabilitySlots(categoryId: CategoryId)

    suspend fun scheduleAppointment(
        categoryId: CategoryId,
        providerId: UUID,
        slot: Instant,
    ): Appointment

    suspend fun cancelAppointment(id: AppointmentId)
}