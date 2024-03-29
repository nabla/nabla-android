package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.SceneHelpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.core.ui.helpers.ViewModelExtension.factoryFor
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentCategoriesBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.scene.CategorySelectionViewModel.State

internal class CategorySelectionFragment : BookAppointmentBaseFragment(R.layout.nabla_scheduling_fragment_categories) {

    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentCategoriesBinding::bind)
    private val viewModel: CategorySelectionViewModel by viewModels {
        factoryFor { CategorySelectionViewModel(nablaClient = nablaClient) }
    }
    private val adapter = AppointmentCategoryAdapter(::onClickCategory)
    private val showLocationHint: Boolean by lazy {
        requireArguments().getBoolean(ARG_SHOW_LOCATION_HINT)
    }
    private val locationType: AppointmentLocationType by lazy {
        requireAppointmentLocationType()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }

        binding.recyclerView.apply {
            adapter = this@CategorySelectionFragment.adapter
            addItemDecoration(VerticalOffsetsItemDecoration())
        }
        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.errorLayout.root.isVisible = state is State.Error
            binding.progressBar.isVisible = state is State.Loading
            binding.recyclerView.isVisible = state is State.Loaded
            binding.locationHint.isVisible = state is State.Loaded && showLocationHint
            binding.nablaNoCategoryText.isVisible = state is State.Empty

            when (state) {
                is State.Error -> {
                    binding.errorLayout.bind(state.errorUiModel, viewModel::onClickRetry)
                }
                is State.Loaded -> {
                    adapter.submitList(state.items)
                }
                State.Loading, State.Empty -> Unit
            }
        }
        binding.locationHint.text = getString(
            when (locationType) {
                AppointmentLocationType.REMOTE -> R.string.nabla_scheduling_remote_location_hint
                AppointmentLocationType.PHYSICAL -> R.string.nabla_scheduling_physical_location_hint
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    private fun onClickCategory(appointmentCategory: AppointmentCategory) {
        hostActivity().goToTimeSlots(locationType, appointmentCategory.id)
    }

    internal companion object {
        private const val ARG_SHOW_LOCATION_HINT = "ARG_SHOW_LOCATION_HINT"

        fun newInstance(
            locationType: AppointmentLocationType,
            showLocationHint: Boolean,
            sdkName: String,
        ) = CategorySelectionFragment().apply {
            arguments = bundleOf(ARG_SHOW_LOCATION_HINT to showLocationHint)
            setAppointmentLocationType(locationType)
            setSdkName(sdkName)
        }
    }
}
