package com.nabla.sdk.scheduling.domain.entity

import java.math.BigDecimal

public data class Price(
    val amount: BigDecimal,
    val currencyCode: String,
) {
    public companion object
}
