package com.nabla.sdk.scheduling.scene.slots

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import com.nabla.sdk.core.ui.helpers.canScrollDown
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentTimeSlotsBinding
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.BookAppointmentBaseFragment
import com.nabla.sdk.scheduling.scene.VerticalOffsetsItemDecoration

internal class TimeSlotsFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_time_slots
) {
    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentTimeSlotsBinding::bind)
    private val viewModel: TimeSlotsViewModel by viewModels {
        factoryFor { TimeSlotsViewModel(categoryId = getCategoryId(), nablaClient = nablaClient) }
    }
    private val adapter by lazy {
        TimeSlotsAdapter(
            onDaySlotsClicked = viewModel::onDaySlotsClicked,
            onSlotClicked = viewModel::onSlotClicked,
            onBindLoading = { viewModel.onListReachedBottom() }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressed() }
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

    private fun getCategoryId(): CategoryId {
        val stringId = requireArguments().getString(ARG_CATEGORY_ID) ?: throw
        IllegalStateException("Missing Category Id").asNablaInternal()
        return CategoryId(Uuid.fromString(stringId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    internal companion object {
        private const val ARG_CATEGORY_ID = "ARG_CATEGORY_ID"

        internal fun newInstance(categoryId: CategoryId, sdkName: String): TimeSlotsFragment {
            val args = Bundle()
            args.putString(ARG_CATEGORY_ID, categoryId.value.toString())
            val fragment = TimeSlotsFragment()
            fragment.arguments = args
            fragment.setSdkName(sdkName)
            return fragment
        }
    }
}
