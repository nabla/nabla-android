package com.nabla.sdk.scheduling.scene.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nabla.sdk.scheduling.SchedulingInternalModule

internal class AppointmentsViewModel(
    schedulingClient: SchedulingInternalModule,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val showNavigation = AppointmentsFragment.Builder.showNavigationFromSavedStateHandle(savedStateHandle)

    val isRefreshingFlow = schedulingClient.isRefreshingAppointments()
}
