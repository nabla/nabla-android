package com.nabla.sdk.scheduling.domain.entity

internal data class Address(
    val address: String,
    val zipCode: String,
    val city: String,
    val state: String?,
    val country: String?,
    val extraDetails: String?
) {
    companion object
}
