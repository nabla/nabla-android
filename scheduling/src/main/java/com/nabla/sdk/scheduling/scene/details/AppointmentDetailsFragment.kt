package com.nabla.sdk.scheduling.scene.details

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.evaluate
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentDetailsBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.address
import com.nabla.sdk.scheduling.scene.SchedulingBaseFragment
import com.nabla.sdk.scheduling.scene.details.AppointmentDetailsViewModel.Event
import com.nabla.sdk.scheduling.scene.details.AppointmentDetailsViewModel.State
import com.nabla.sdk.scheduling.scene.requireAppointmentId
import com.nabla.sdk.scheduling.scene.setAppointmentId

internal class AppointmentDetailsFragment : SchedulingBaseFragment(
    R.layout.nabla_scheduling_fragment_appointment_details
) {
    private val binding by viewBinding(NablaSchedulingFragmentAppointmentDetailsBinding::bind)
    private val viewModel: AppointmentDetailsViewModel by viewModels {
        factoryFor {
            AppointmentDetailsViewModel(
                appointmentId = requireAppointmentId(),
                nablaClient = getNablaInstanceByName(),
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.nablaCancelAppointmentButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_title))
                .setNegativeButton(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_negative) { _, _ -> /* no-op */ }
                .setPositiveButton(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_positive) { _, _ ->
                    viewModel.onCancelClicked()
                }
                .create()
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.bind(state, viewModel::onClickRetry)
        }

        viewLifecycleOwner.launchCollect(viewModel.eventsFlow) { event ->
            when (event) {
                is Event.FailedCancelling -> Toast.makeText(context, event.message.evaluate(this), Toast.LENGTH_LONG).show()
                Event.AppointmentCancelled -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    internal companion object {
        internal fun newInstance(
            appointmentId: AppointmentId,
            sdkName: String = NablaClient.DEFAULT_NAME,
        ) = AppointmentDetailsFragment().apply {
            setAppointmentId(appointmentId)
            setSdkName(sdkName)
        }
    }
}

internal fun NablaSchedulingFragmentAppointmentDetailsBinding.bind(state: State, onRetryListener: () -> Unit = {}) {
    nablaAppointmentSummary.isVisible = state.appointment != null
    nablaCancelAppointmentButton.isVisible = state is State.Loaded && state.cancelAvailable
    progressBar.isVisible = state is State.Loading
    errorLayout.root.isVisible = state is State.Error

    state.appointment?.let {
        nablaAppointmentSummary.bind(
            it.location.type,
            it.provider,
            it.scheduledAt,
            it.location.address,
        )
    }
    if (state is State.Error) {
        errorLayout.bind(state.errorUiModel, onRetryListener)
    }
}
