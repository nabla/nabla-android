package com.nabla.sdk.scheduling.ui.viewmodel

import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import com.nabla.sdk.scheduling.SchedulingPrivateClient
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import java.util.UUID

internal open class SchedulingPrivateClientAdapter : SchedulingPrivateClient {
    override suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>> =
        runCatchingCancellable { error("Not mocked") }

    override suspend fun getAppointmentLocationTypes(): Result<Set<AppointmentLocationType>> {
        error("Not mocked")
    }

    override fun watchAvailabilitySlots(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId
    ): Flow<PaginatedContent<List<AvailabilitySlot>>> =
        error("Not mocked")

    override fun isRefreshingAppointments(): Flow<Boolean> = flowOf(false)

    override fun watchPastAppointments(): Flow<Response<PaginatedContent<List<Appointment>>>> =
        error("Not mocked")

    override fun watchUpcomingAppointments(): Flow<Response<PaginatedContent<List<Appointment>>>> =
        error("Not mocked")

    override suspend fun getAppointmentConfirmationConsents(locationType: AppointmentLocationType): Result<AppointmentConfirmationConsents> {
        error("Not mocked")
    }

    override suspend fun createPendingAppointment(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant
    ): Result<Appointment> {
        error("Not mocked")
    }

    override suspend fun cancelAppointment(id: AppointmentId): Result<Unit> =
        runCatchingCancellable { error("Not mocked") }

    override suspend fun getAppointment(id: AppointmentId): Result<Appointment> {
        error("Not mocked")
    }

    override val paymentActivityContract = null
    override suspend fun schedulePendingAppointment(appointmentId: AppointmentId): Result<Appointment> {
        error("Not mocked")
    }
}
