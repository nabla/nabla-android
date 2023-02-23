package com.nabla.sdk.scheduling.domain.entity

import androidx.annotation.VisibleForTesting
import java.math.BigDecimal

public data class Price(
    val amount: BigDecimal,
    val currencyCode: String,
) {
    @VisibleForTesting
    public companion object
}
