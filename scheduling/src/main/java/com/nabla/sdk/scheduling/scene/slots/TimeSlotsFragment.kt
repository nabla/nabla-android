package com.nabla.sdk.scheduling.scene.slots

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentTimeSlotsBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.scene.BookAppointmentBaseFragment
import com.nabla.sdk.scheduling.scene.VerticalOffsetsItemDecoration
import com.nabla.sdk.scheduling.scene.requireAppointmentCategoryId
import com.nabla.sdk.scheduling.scene.requireAppointmentLocationType
import com.nabla.sdk.scheduling.scene.setAppointmentCategoryId
import com.nabla.sdk.scheduling.scene.setAppointmentLocationType
import com.nabla.sdk.scheduling.schedulingInternalModule

internal class TimeSlotsFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_time_slots
) {
    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentTimeSlotsBinding::bind)
    private val viewModel: TimeSlotsViewModel by viewModels {
        factoryFor {
            TimeSlotsViewModel(
                locationType = locationType,
                categoryId = categoryId,
                schedulingModule = nablaClient.schedulingInternalModule,
                logger = nablaClient.coreContainer.logger,
            )
        }
    }
    private val adapter by lazy {
        TimeSlotsAdapter(
            onDaySlotsClicked = viewModel::onDaySlotsClicked,
            onSlotClicked = viewModel::onSlotClicked,
            onBindLoading = { viewModel.onListReachedBottom() }
        )
    }

    private val locationType: AppointmentLocationType by lazy {
        requireAppointmentLocationType()
    }

    private val categoryId: AppointmentCategoryId by lazy {
        requireAppointmentCategoryId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }
        binding.nablaSlotsContinueButton.setOnClickListener { viewModel.onConfirmClicked() }

        binding.recyclerView.apply {
            adapter = this@TimeSlotsFragment.adapter
            addItemDecoration(VerticalOffsetsItemDecoration())
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (!recyclerView.canScrollDown()) {
                            viewModel.onListReachedBottom()
                        }
                    }
                }
            )
        }
        viewLifecycleOwner.launchCollect(viewModel.eventsFlow) { event ->
            when (event) {
                is TimeSlotsViewModel.Event.GoToConfirmation -> hostActivity().goToConfirmation(
                    event.locationType,
                    event.categoryId,
                    event.providerId,
                    event.slot,
                )
                is TimeSlotsViewModel.Event.ErrorAlert.Pagination -> {
                    Toast.makeText(requireContext(), event.errorMessageRes, Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.progressBar.isVisible = state is TimeSlotsViewModel.State.Loading
            binding.errorLayout.root.isVisible = state is TimeSlotsViewModel.State.Error
            binding.nablaSlotsContinueButton.isEnabled = state is TimeSlotsViewModel.State.Loaded && state.canSubmit
            binding.nablaNoAvailabilityText.isVisible = state is TimeSlotsViewModel.State.Empty

            when (state) {
                is TimeSlotsViewModel.State.Error -> {
                    binding.errorLayout.bind(state.errorUiModel) {
                        viewModel.onClickRetry()
                    }
                }
                is TimeSlotsViewModel.State.Loaded -> {
                    adapter.submitList(state.items)
                }
                TimeSlotsViewModel.State.Empty, TimeSlotsViewModel.State.Loading -> Unit /* no-op */
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    internal companion object {
        internal fun newInstance(
            locationType: AppointmentLocationType,
            categoryId: AppointmentCategoryId,
            sdkName: String
        ) = TimeSlotsFragment().apply {
            setAppointmentLocationType(locationType)
            setAppointmentCategoryId(categoryId)
            setSdkName(sdkName)
        }
    }
}
