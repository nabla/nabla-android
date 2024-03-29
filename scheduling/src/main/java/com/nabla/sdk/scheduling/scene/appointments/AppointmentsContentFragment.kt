package com.nabla.sdk.scheduling.scene.appointments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.evaluate
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.RecyclerViewExtension.canScrollDown
import com.nabla.sdk.core.ui.helpers.SceneHelpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.requireSdkName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.core.ui.helpers.ViewModelExtension.savedStateFactoryFor
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentsContentBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.scene.SchedulingBaseFragment
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.Companion.APPOINTMENT_TYPE_ARG
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.Event
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel.State
import com.nabla.sdk.scheduling.scene.details.AppointmentDetailsActivity
import com.nabla.sdk.scheduling.schedulingPrivateClient
import kotlinx.datetime.Clock

internal class AppointmentsContentFragment : SchedulingBaseFragment(
    R.layout.nabla_scheduling_fragment_appointments_content,
) {

    private val nablaClient: NablaClient = getNablaInstanceByName()

    private val binding by viewBinding(NablaSchedulingFragmentAppointmentsContentBinding::bind)

    private val viewModel: AppointmentsContentViewModel by viewModels {
        savedStateFactoryFor { handle ->
            AppointmentsContentViewModel(
                schedulingPrivateClient = nablaClient.schedulingPrivateClient,
                videoCallInternalClient = nablaClient.coreContainer.videoCallModule?.internalClient,
                logger = nablaClient.coreContainer.logger,
                configuration = nablaClient.coreContainer.configuration,
                clock = Clock.System,
                appointmentType = handle.get(APPOINTMENT_TYPE_ARG) ?: error("No appointment type specified"),
            )
        }
    }

    private val appointmentsAdapter by lazy {
        AppointmentsAdapter(
            onJoinClicked = viewModel::onJoinClicked,
            onDetailsClicked = ::openAppointmentDetailsScreen,
            onJoinExternalClicked = ::openExternalLink,
        )
    }

    private fun openAppointmentDetailsScreen(appointmentId: AppointmentId) {
        startActivity(AppointmentDetailsActivity.newIntent(requireContext(), appointmentId, requireSdkName()))
    }

    private fun openExternalLink(url: Uri) {
        try {
            context?.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url.uri)))
        } catch (e: Exception) {
            viewModel.onFailedToOpenExternalLink(e)
        }
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
                    Toast.makeText(context, event.errorMessage.evaluate(this), Toast.LENGTH_LONG).show()
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
                },
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
