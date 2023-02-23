package com.nabla.sdk.scheduling.scene.slots

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.evaluate
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.RecyclerViewExtension.canScrollDown
import com.nabla.sdk.core.ui.helpers.SceneHelpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.core.ui.helpers.ViewModelExtension.savedStateFactoryFor
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
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
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsViewModel.State
import com.nabla.sdk.scheduling.schedulingPrivateClient

internal class TimeSlotsFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_time_slots
) {
    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentTimeSlotsBinding::bind)
    private val viewModel: TimeSlotsViewModel by viewModels {
        savedStateFactoryFor {
            TimeSlotsViewModel(
                locationType = locationType,
                categoryId = categoryId,
                schedulingPrivateClient = nablaClient.schedulingPrivateClient,
                logger = nablaClient.coreContainer.logger,
                handle = it,
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
                    event.pendingAppointmentId,
                )
                is TimeSlotsViewModel.Event.ShowMessage -> {
                    Toast.makeText(requireContext(), event.message.evaluate(this), Toast.LENGTH_SHORT).show()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.progressBar.isVisible = state is State.Loading
            binding.errorLayout.root.isVisible = state is State.Error
            binding.nablaSlotsContinueButton.isEnabled = state is State.Loaded && state.canSubmit
            binding.nablaNoAvailabilityText.isVisible = state is State.Empty
            binding.recyclerView.isVisible = state is State.Loaded

            when (state) {
                is State.Error -> {
                    binding.errorLayout.bind(state.errorUiModel) {
                        viewModel.onClickRetry()
                    }
                }
                is State.Loaded -> {
                    adapter.submitList(state.items)
                }
                State.Empty, State.Loading -> Unit /* no-op */
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
