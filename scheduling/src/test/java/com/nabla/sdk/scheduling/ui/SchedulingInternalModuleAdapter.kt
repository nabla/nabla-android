package com.nabla.sdk.scheduling.ui

import android.content.Context
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.scheduling.SchedulingInternalModule
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal open class SchedulingInternalModuleAdapter : SchedulingInternalModule {
    override suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>> = runCatchingCancellable { error("Not mocked") }
    override fun watchAvailabilitySlots(categoryId: CategoryId): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>> = error("Not mocked")
    override fun watchPastAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> = error("Not mocked")
    override fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> = error("Not mocked")
    override suspend fun scheduleAppointment(categoryId: CategoryId, providerId: UUID, slot: Instant): Result<Appointment> =
        runCatchingCancellable { error("Not mocked") }

    override suspend fun cancelAppointment(id: AppointmentId): Result<Unit> = runCatchingCancellable { error("Not mocked") }
    override fun openScheduleAppointmentActivity(context: Context) = error("Not mocked")
}
