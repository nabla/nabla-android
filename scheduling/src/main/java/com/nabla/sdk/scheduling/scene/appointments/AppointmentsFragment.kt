package com.nabla.sdk.scheduling.scene.appointments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.savedStateFactoryFor
import com.nabla.sdk.core.ui.helpers.sdkNameOrDefault
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.helpers.viewLifeCycleScope
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentsBinding
import com.nabla.sdk.scheduling.scene.SchedulingBaseFragment
import com.nabla.sdk.scheduling.schedulingClient
import com.nabla.sdk.scheduling.schedulingPrivateClient
import kotlinx.coroutines.launch

public class AppointmentsFragment : SchedulingBaseFragment(
    R.layout.nabla_scheduling_fragment_appointments
) {
    private val nablaClient: NablaClient = getNablaInstanceByName()

    private val binding by viewBinding(NablaSchedulingFragmentAppointmentsBinding::bind)
    private val viewModel: AppointmentsViewModel by viewModels {
        savedStateFactoryFor { handle ->
            AppointmentsViewModel(
                schedulingPrivateClient = nablaClient.schedulingPrivateClient,
                savedStateHandle = handle,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewPager()

        binding.nablaBookAppointmentButton.setOnClickListener {
            nablaClient.schedulingClient.openScheduleAppointmentActivity(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.toolbar.isVisible = viewModel.showNavigation

        viewLifeCycleScope.launch {
            viewModel.isRefreshingFlow.collect { isRefreshing ->
                binding.toolbarProgressBar.isVisible = isRefreshing
            }
        }
    }

    private fun setupViewPager() {
        binding.nablaAppointmentsPager.apply {
            adapter = object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int = AppointmentType.values().size

                override fun createFragment(position: Int): Fragment {
                    return AppointmentsContentFragment.createFor(AppointmentType.values()[position], sdkNameOrDefault())
                }
            }

            TabLayoutMediator(binding.nablaAppointmentsTabLayout, binding.nablaAppointmentsPager) { tab, position ->
                tab.setText(AppointmentType.values()[position].titleRes)
            }.attach()
        }
    }

    @Suppress("UNUSED")
    public class Builder internal constructor() {
        private var showNavigation = true

        /**
         * Call this to display or hide the navigation bar (Toolbar) displayed at the top. Navigation is shown
         * by default but if you want to embed that screen into your own navigation you can call this
         * method to hide it.
         */
        public fun setShowNavigation(showNavigation: Boolean) {
            this.showNavigation = showNavigation
        }

        internal fun build(): AppointmentsFragment {
            return AppointmentsFragment().apply {
                arguments = newArgsBundle(showNavigation)
            }
        }

        internal companion object {
            private const val SHOW_NAVIGATION_ARG_KEY = "showNavigation"

            private fun newArgsBundle(
                showNavigation: Boolean,
            ): Bundle = Bundle().apply {
                putBoolean(SHOW_NAVIGATION_ARG_KEY, showNavigation)
            }

            internal fun showNavigationFromSavedStateHandle(savedStateHandle: SavedStateHandle): Boolean {
                return savedStateHandle.get(SHOW_NAVIGATION_ARG_KEY) ?: true
            }
        }
    }

    public companion object {
        public fun newInstance(
            init: (Builder.() -> Unit)? = null,
        ): AppointmentsFragment = newInstance(NablaClient.DEFAULT_NAME, init)

        public fun newInstance(
            sdkName: String,
            init: (Builder.() -> Unit)? = null,
        ): AppointmentsFragment {
            val builder = Builder()
            init?.invoke(builder)
            return builder.build().also {
                it.setSdkName(sdkName)
            }
        }
    }
}
