package com.nabla.sdk.scheduling.domain.entity

import androidx.annotation.VisibleForTesting
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.scheduling.PaymentActivityContract
import kotlinx.datetime.Instant

/**
 * An appointment that is not yet confirmed (status is `PENDING` on the REST API).
 * The SDK will either:
 *   - confirm it right after its creation if it does not require payment,
 * OR
 *   - pass it to your implementation of [PaymentActivityContract].
 *
 *   @param id the id you can use to reference this appointment when communicating with our server.
 *   @param provider the provider with which the current patient is about to have an appointment.
 *   @param scheduledAt start time of the appointment.
 *   @param location where will the appointment take place. Either a physical address or online.
 *   @param price the price for this appointment as decided by your payments configuration
 *          (e.g. as returned by your webhook). Will be null if your payments configuration
 *          is disabled or if it decided to not require a payment.
 */
public data class PendingAppointment(
    val id: AppointmentId,
    val provider: Provider,
    val scheduledAt: Instant,
    val location: AppointmentLocation,
    val price: Price?,
) {
    @VisibleForTesting
    public companion object
}
