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
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentCategoriesBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.scene.CategorySelectionViewModel.State

internal class CategorySelectionFragment : BookAppointmentBaseFragment(R.layout.nabla_scheduling_fragment_categories) {

    private val nablaClient: NablaClient = getNablaInstanceByName()
    private val binding by viewBinding(NablaSchedulingFragmentCategoriesBinding::bind)
    private val viewModel: CategorySelectionViewModel by viewModels {
        factoryFor { CategorySelectionViewModel(nablaClient = nablaClient) }
    }
    private val adapter = AppointmentCategoryAdapter(::onClickCategory)

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
    }

    private fun onClickCategory(appointmentCategory: AppointmentCategory) {
        hostActivity().goToTimeSlots(appointmentCategory.id)
    }

    internal companion object {
        fun newInstance(sdkName: String): CategorySelectionFragment {
            return CategorySelectionFragment().apply { setSdkName(sdkName) }
        }
    }
}
