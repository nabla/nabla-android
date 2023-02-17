package com.nabla.sdk.scheduling

import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.scheduling.domain.entity.PendingAppointment

/**
 * The contract for the payment step in the scheduling funnel.
 * Your payment activity will take a [PendingAppointment] and should return a [PaymentActivityContract.Result].
 *
 * Nabla Scheduling SDK will start your activity for result after the user has confirmed all the details in [PendingAppointment].
 */
public abstract class PaymentActivityContract : ActivityResultContract<PendingAppointment, PaymentActivityContract.Result>() {
    public sealed interface Result {
        /**
         * Payment was completed successfully, can proceed confirming the appointment.
         *
         * By the time you finish your activity with this as a result
         * __your backend should have already signaled to Nabla's backend the payment success__.
         */
        public object Succeeded : Result

        /**
         * Payment did not complete for whatever reason (might be just the user pressing the back button).
         * User will be able to try again.
         */
        public object ShouldRetry : Result
    }
}
