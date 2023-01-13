package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentLocationsBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.scene.LocationSelectionViewModel.State

internal class LocationSelectionFragment : BookAppointmentBaseFragment(R.layout.nabla_scheduling_fragment_locations) {

    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentLocationsBinding::bind)
    private val viewModel: LocationSelectionViewModel by viewModels {
        factoryFor { LocationSelectionViewModel(nablaClient = nablaClient) }
    }
    private val adapter = AppointmentLocationAdapter(::onClickLocationType)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }

        binding.recyclerView.apply {
            adapter = this@LocationSelectionFragment.adapter
            addItemDecoration(VerticalOffsetsItemDecoration())
        }
        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.errorLayout.root.isVisible = state is State.Error
            binding.progressBar.isVisible = state is State.Loading
            binding.recyclerView.isVisible = state is State.Loaded
            binding.nablaNoLocationText.isVisible = state is State.Empty

            when (state) {
                is State.Error -> {
                    binding.errorLayout.bind(state.errorUiModel, viewModel::onClickRetry)
                }
                is State.Loaded -> {
                    val singleChoice = state.items.singleOrNull()
                    if (singleChoice != null) {
                        hostActivity().goToCategorySelection(singleChoice, singleLocation = true)
                    } else {
                        adapter.submitList(state.items)
                    }
                }
                State.Loading, State.Empty -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    private fun onClickLocationType(locationType: AppointmentLocationType) {
        hostActivity().goToCategorySelection(locationType)
    }

    internal companion object {
        fun newInstance(sdkName: String): LocationSelectionFragment {
            return LocationSelectionFragment().apply { setSdkName(sdkName) }
        }
    }
}
