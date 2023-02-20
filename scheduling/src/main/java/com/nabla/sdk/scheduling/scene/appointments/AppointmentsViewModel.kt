package com.nabla.sdk.scheduling.scene.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nabla.sdk.scheduling.SchedulingPrivateClient

internal class AppointmentsViewModel(
    schedulingPrivateClient: SchedulingPrivateClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val showNavigation = AppointmentsFragment.Builder.showNavigationFromSavedStateHandle(savedStateHandle)

    val isRefreshingFlow = schedulingPrivateClient.isRefreshingAppointments()
}
