package com.nabla.sdk.scheduling.data.apollo

import com.nabla.sdk.core.data.apollo.CoreGqlMapper
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentCategoryFragment
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentFragment
import com.nabla.sdk.scheduling.graphql.fragment.AvailabilitySlotFragment
import kotlin.time.Duration.Companion.minutes

internal class GqlMapper(
    private val coreGqlMapper: CoreGqlMapper,
) {
    fun mapToAppointment(fragment: AppointmentFragment): Appointment {
        val upcoming = fragment.state.onUpcomingAppointment
        return when {
            upcoming != null -> {
                Appointment.Upcoming(
                    AppointmentId(fragment.id),
                    coreGqlMapper.mapToProvider(fragment.provider.providerFragment),
                    fragment.scheduledAt,
                    upcoming.livekitRoom?.livekitRoomFragment?.let(coreGqlMapper::mapToLivekitRoom),
                )
            }
            else -> {
                Appointment.Finalized(
                    AppointmentId(fragment.id),
                    coreGqlMapper.mapToProvider(fragment.provider.providerFragment),
                    fragment.scheduledAt,
                )
            }
        }
    }

    fun mapToAppointmentCategory(fragment: AppointmentCategoryFragment): AppointmentCategory {
        return AppointmentCategory(
            CategoryId(fragment.id),
            fragment.name,
            fragment.callDurationMinutes.minutes,
        )
    }

    fun mapToAvailabilitySlot(fragment: AvailabilitySlotFragment): AvailabilitySlot {
        return AvailabilitySlot(
            startAt = fragment.startAt,
            providerId = fragment.provider.id,
        )
    }
}
