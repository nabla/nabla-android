package com.nabla.sdk.scheduling

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.auth.ensureAuthenticatedOrThrow
import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.core.domain.helper.makePaginatedFlow
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.injection.SchedulingContainer
import com.nabla.sdk.scheduling.scene.ScheduleAppointmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.util.UUID

internal class NablaSchedulingClientImpl(
    coreContainer: CoreContainer,
) : NablaSchedulingClient, SchedulingModule, SchedulingInternalModule {

    private val schedulingContainer = SchedulingContainer(coreContainer)
    private val appointmentRepository = schedulingContainer.appointmentRepository
    private val name = coreContainer.name

    override suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.ensureAuthenticatedOrThrow()
            appointmentRepository.getCategories()
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun getAppointmentLocationTypes(): Result<Set<AppointmentLocationType>> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.ensureAuthenticatedOrThrow()
            appointmentRepository.getLocationTypes()
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override fun watchAvailabilitySlots(
        categoryId: AppointmentCategoryId,
    ): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>> {
        return makePaginatedFlow(
            appointmentRepository.watchAvailabilitySlots(categoryId),
            { appointmentRepository.loadMoreAvailabilitySlots(categoryId) },
            schedulingContainer.nablaExceptionMapper,
            schedulingContainer.sessionClient,
        )
    }

    override fun watchPastAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> {
        return makePaginatedFlow(
            appointmentRepository.watchPastAppointments(),
            appointmentRepository::loadMorePastAppointments,
            schedulingContainer.nablaExceptionMapper,
            schedulingContainer.sessionClient,
        )
    }

    override fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> {
        return makePaginatedFlow(
            appointmentRepository.watchUpcomingAppointments(),
            appointmentRepository::loadMoreUpcomingAppointments,
            schedulingContainer.nablaExceptionMapper,
            schedulingContainer.sessionClient,
        )
    }

    override suspend fun getAppointmentConfirmationConsents(locationType: AppointmentLocationType): Result<AppointmentConfirmationConsents> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.ensureAuthenticatedOrThrow()
            appointmentRepository.getAppointmentConfirmationConsents(locationType)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun scheduleAppointment(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant
    ): Result<Appointment> {
        return runCatchingCancellable {
            appointmentRepository.scheduleAppointment(locationType, categoryId, providerId, slot)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun cancelAppointment(id: AppointmentId): Result<Unit> {
        return runCatchingCancellable {
            appointmentRepository.cancelAppointment(id)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun getAppointment(id: AppointmentId): Result<Appointment> {
        return runCatchingCancellable {
            appointmentRepository.getAppointment(id)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override fun openScheduleAppointmentActivity(context: Context) {
        context.startActivity(
            ScheduleAppointmentActivity.newIntent(
                context,
                name,
            ).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}
