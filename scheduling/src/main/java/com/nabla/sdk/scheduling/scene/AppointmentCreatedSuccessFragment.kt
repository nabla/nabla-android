package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.view.View
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentSuccessBinding

internal class AppointmentCreatedSuccessFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_appointment_success
) {
    private val binding by viewBinding(NablaSchedulingFragmentAppointmentSuccessBinding::bind)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nablaAppointmentSuccessButton.setOnClickListener {
            hostActivity().finish()
        }
    }
}
