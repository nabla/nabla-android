package com.nabla.sdk.scheduling.domain.entity

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import kotlinx.parcelize.Parcelize

@Parcelize
public data class Address(
    val address: String,
    val zipCode: String,
    val city: String,
    val state: String?,
    val country: String?,
    val extraDetails: String?,
) : Parcelable {
    @VisibleForTesting
    public companion object
}
