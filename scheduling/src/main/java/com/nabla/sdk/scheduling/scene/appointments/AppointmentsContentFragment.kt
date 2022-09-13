package com.nabla.sdk.scheduling.scene.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.savedStateFactoryFor
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentsContentBinding
import com.nabla.sdk.scheduling.scene.SchedulingBaseFragment
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.Companion.APPOINTMENT_TYPE_ARG
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.Event
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.State

internal class AppointmentsContentFragment : SchedulingBaseFragment() {

    private val nablaClient: NablaClient = getNablaInstanceByName()

    private val binding by viewBinding(NablaSchedulingFragmentAppointmentsContentBinding::bind)

    private val viewModel: AppointmentsContentViewModel by viewModels {
        savedStateFactoryFor { handle ->
            AppointmentsContentViewModel(
                nablaClient = nablaClient,
                handle,
            )
        }
    }

    private val appointmentsAdapter by lazy {
        AppointmentsAdapter(
            onCancelClicked = viewModel::onCancelClicked,
            onJoinClicked = viewModel::onJoinClicked,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return NablaSchedulingFragmentAppointmentsContentBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.nablaIncludedErrorLayout.nablaErrorRetryButton.setOnClickListener { viewModel.onRetryClicked() }

        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.nablaIncludedErrorLayout.root.isVisible = state is State.Error
            binding.nablaLoadingProgressBar.isVisible = state is State.Loading
            binding.nablaAppointmentsRecyclerView.isVisible = state is State.Loaded
            binding.nablaNoAppointmentText.isVisible = state is State.Empty

            when (state) {
                is State.Empty -> {
                    binding.nablaNoAppointmentText.setText(state.placeholderTextRes)
                }
                is State.Error -> {
                    binding.nablaIncludedErrorLayout.bind(state.errorUiModel, viewModel::onRetryClicked)
                }
                is State.Loaded -> {
                    appointmentsAdapter.submitList(state.items)
                }
                State.Loading -> Unit /* no-op */
            }
        }

        viewLifecycleOwner.launchCollect(viewModel.eventsFlow) { event ->
            when (event) {
                is Event.FailedCancelling -> {
                    Toast.makeText(context, event.errorMessage, Toast.LENGTH_LONG).show()
                }
                Event.FailedPagination -> {
                    Toast.makeText(context, R.string.nabla_scheduling_appointments_pagination_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.nablaAppointmentsRecyclerView.apply {
            adapter = appointmentsAdapter
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(nablaAppointmentsRecyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (!nablaAppointmentsRecyclerView.canScrollDown()) {
                            viewModel.onListReachedBottom()
                        }
                    }
                }
            )
        }
    }

    companion object {
        fun createFor(appointmentType: AppointmentType, sdkName: String): AppointmentsContentFragment {
            return AppointmentsContentFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(APPOINTMENT_TYPE_ARG, appointmentType)
                }
                setSdkName(sdkName)
            }
        }
    }
}
