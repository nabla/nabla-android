package com.nabla.sdk.scheduling.data.apollo

import com.nabla.sdk.core.data.apollo.CoreGqlMapper
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.Price
import com.nabla.sdk.scheduling.graphql.AppointmentConfirmationConsentsQuery
import com.nabla.sdk.scheduling.graphql.fragment.AddressFragment
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentCategoryFragment
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentFragment
import com.nabla.sdk.scheduling.graphql.fragment.AvailabilitySlotFragment
import com.nabla.sdk.scheduling.graphql.fragment.PriceFragment
import kotlin.time.Duration.Companion.minutes

internal class GqlMapper(
    private val coreGqlMapper: CoreGqlMapper,
    private val logger: Logger,
) {
    fun mapToAppointment(fragment: AppointmentFragment): Appointment {
        val location = mapLocation(fragment.location)
        return Appointment(
            id = AppointmentId(fragment.id),
            provider = coreGqlMapper.mapToProvider(fragment.provider.providerFragment),
            scheduledAt = fragment.scheduledAt,
            state = mapAppointmentState(fragment.state),
            location = location,
            price = fragment.price?.priceFragment?.let { mapPrice(it) },
        )
    }

    private fun mapAppointmentState(state: AppointmentFragment.State) =
        when {
            state.onUpcomingAppointment != null -> AppointmentState.Upcoming
            state.onFinalizedAppointment != null -> AppointmentState.Finalized
            state.onPendingAppointment != null -> AppointmentState.Pending(
                requiredPrice = state.onPendingAppointment.schedulingPaymentRequirement?.price?.priceFragment?.let(::mapPrice)
            )
            else -> {
                logger.error("Unknown appointment state $state — considering it as Upcoming")
                AppointmentState.Upcoming
            }
        }

    private fun mapPrice(price: PriceFragment) = Price(price.amount, price.currencyCode)

    private fun mapLocation(location: AppointmentFragment.Location): AppointmentLocation {
        location.onPhysicalAppointmentLocation?.let {
            val addressFragment = it.address.addressFragment
            return AppointmentLocation.Physical(mapAddress(addressFragment))
        }
        location.onRemoteAppointmentLocation?.let { remote ->
            return remote.externalCallUrl?.let { stringUrl ->
                AppointmentLocation.Remote.External(Uri(stringUrl))
            } ?: AppointmentLocation.Remote.Nabla(
                remote.livekitRoom?.livekitRoomFragment?.let { coreGqlMapper.mapToVideoCallRoom(it) }
            )
        }
        logger.error("Unknown appointment location mapping for $location")
        return AppointmentLocation.Unknown
    }

    private fun mapAddress(addressFragment: AddressFragment): Address {
        return Address(
            address = addressFragment.address,
            zipCode = addressFragment.zipCode,
            city = addressFragment.city,
            state = addressFragment.state,
            country = addressFragment.country,
            extraDetails = addressFragment.extraDetails
        )
    }

    fun mapToAppointmentCategory(fragment: AppointmentCategoryFragment): AppointmentCategory {
        return AppointmentCategory(
            AppointmentCategoryId(fragment.id),
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

    fun mapToAppointmentConfirmationConsents(
        gqlData: AppointmentConfirmationConsentsQuery.AppointmentConfirmationConsents,
        locationType: AppointmentLocationType,
    ): AppointmentConfirmationConsents {
        val htmlConsents = mutableListOf<String>()
        val checkAndAdd: (String) -> Unit = { htmlString ->
            if (htmlString.isNotBlank()) htmlConsents.add(htmlString)
        }
        when (locationType) {
            AppointmentLocationType.PHYSICAL -> {
                checkAndAdd(gqlData.physicalFirstConsentHtml)
                checkAndAdd(gqlData.physicalSecondConsentHtml)
            }
            AppointmentLocationType.REMOTE -> {
                checkAndAdd(gqlData.firstConsentHtml)
                checkAndAdd(gqlData.secondConsentHtml)
            }
        }
        return AppointmentConfirmationConsents(htmlConsents)
    }
}
