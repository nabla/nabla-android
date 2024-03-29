package com.nabla.sdk.scheduling

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.CoroutineHelpers.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.SchedulingInternalClient
import com.nabla.sdk.core.domain.boundary.SchedulingModule
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.helper.AuthHelper.authenticatable
import com.nabla.sdk.core.domain.helper.FlowConnectionStateAwareHelper.restartWhenConnectionReconnects
import com.nabla.sdk.core.domain.helper.PaginationHelper.wrapAsPaginatedContent
import com.nabla.sdk.core.domain.helper.PaginationHelper.wrapAsResponsePaginatedContent
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import java.util.UUID

internal class SchedulingModuleImpl(
    coreContainer: CoreContainer,
) : SchedulingModule, NablaSchedulingClient, SchedulingPrivateClient, SchedulingInternalClient {

    override var paymentActivityContract: PaymentActivityContract? = null
        private set

    private val schedulingContainer = SchedulingContainer(coreContainer)
    private val appointmentRepository = schedulingContainer.appointmentRepository
    private val name = coreContainer.name

    private val areUpcomingAppointmentsRefreshingMutableFlow = MutableStateFlow(false)
    private val arePastAppointmentsRefreshingMutableFlow = MutableStateFlow(false)

    override suspend fun getAppointmentCategories(): Result<List<AppointmentCategory>> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.authenticatableOrThrow()
            appointmentRepository.getCategories()
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun getAppointmentLocationTypes(): Result<Set<AppointmentLocationType>> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.authenticatableOrThrow()
            appointmentRepository.getLocationTypes()
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override fun watchAvailabilitySlots(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
    ): Flow<PaginatedContent<List<AvailabilitySlot>>> {
        return appointmentRepository.watchAvailabilitySlots(locationType, categoryId).wrapAsPaginatedContent(
            { appointmentRepository.loadMoreAvailabilitySlots(locationType, categoryId) },
            schedulingContainer.nablaExceptionMapper,
        ).authenticatable(schedulingContainer.sessionClient)
            .restartWhenConnectionReconnects(schedulingContainer.eventsConnectionStateFlow)
    }

    override fun isRefreshingAppointments(): Flow<Boolean> = arePastAppointmentsRefreshingMutableFlow
        .combine(areUpcomingAppointmentsRefreshingMutableFlow) { pastRefreshing, upcomingRefreshing ->
            pastRefreshing || upcomingRefreshing
        }

    override fun watchPastAppointments(): Flow<Response<PaginatedContent<List<Appointment>>>> {
        return appointmentRepository.watchPastAppointments()
            .wrapAsResponsePaginatedContent(
                appointmentRepository::loadMorePastAppointments,
                schedulingContainer.nablaExceptionMapper,
            )
            .authenticatable(schedulingContainer.sessionClient)
            .restartWhenConnectionReconnects(schedulingContainer.eventsConnectionStateFlow)
            .onEach { response ->
                arePastAppointmentsRefreshingMutableFlow.value = response.refreshingState is RefreshingState.Refreshing
            }
    }

    override fun watchUpcomingAppointments(): Flow<Response<PaginatedContent<List<Appointment>>>> {
        return appointmentRepository.watchUpcomingAppointments()
            .wrapAsResponsePaginatedContent(
                appointmentRepository::loadMoreUpcomingAppointments,
                schedulingContainer.nablaExceptionMapper,
            ).authenticatable(schedulingContainer.sessionClient)
            .restartWhenConnectionReconnects(schedulingContainer.eventsConnectionStateFlow)
            .onEach { response ->
                areUpcomingAppointmentsRefreshingMutableFlow.value = response.refreshingState is RefreshingState.Refreshing
            }
    }

    override suspend fun getAppointmentConfirmationConsents(locationType: AppointmentLocationType): Result<AppointmentConfirmationConsents> {
        return runCatchingCancellable {
            schedulingContainer.sessionClient.authenticatableOrThrow()
            appointmentRepository.getAppointmentConfirmationConsents(locationType)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun createPendingAppointment(
        locationType: AppointmentLocationType,
        categoryId: AppointmentCategoryId,
        providerId: UUID,
        slot: Instant,
    ): Result<Appointment> {
        return runCatchingCancellable {
            appointmentRepository.createPendingAppointment(locationType, categoryId, providerId, slot)
        }.mapFailureAsNablaException(schedulingContainer.nablaExceptionMapper)
    }

    override suspend fun schedulePendingAppointment(
        appointmentId: AppointmentId,
    ): Result<Appointment> {
        return runCatchingCancellable {
            appointmentRepository.schedulePendingAppointment(appointmentId)
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
            },
        )
    }

    override fun registerPaymentActivityContract(contract: PaymentActivityContract) {
        paymentActivityContract = contract
    }

    @NablaInternal
    override val internalClient: SchedulingInternalClient = this
}
