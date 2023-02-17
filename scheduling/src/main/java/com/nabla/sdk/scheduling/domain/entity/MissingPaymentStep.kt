package com.nabla.sdk.scheduling.domain.entity

import com.nabla.sdk.core.domain.entity.NablaException

public class MissingPaymentStep : NablaException(
    message = "Appointment requires payment but no PaymentActivityContract was provided. Make sure you register one before user starts scheduling."
)
