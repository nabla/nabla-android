package com.nabla.sdk.scheduling

import android.content.Context

public interface NablaSchedulingClient {
    public fun openScheduleAppointmentActivity(context: Context)
    public fun registerPaymentActivityContract(contract: PaymentActivityContract)
}
