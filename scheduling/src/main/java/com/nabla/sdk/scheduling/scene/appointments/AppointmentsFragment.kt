package com.nabla.sdk.scheduling.scene.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.sdkNameOrDefault
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentsBinding
import com.nabla.sdk.scheduling.scene.ScheduleAppointmentActivity
import com.nabla.sdk.scheduling.scene.SchedulingBaseFragment

public class AppointmentsFragment : SchedulingBaseFragment() {

    private val binding by viewBinding(NablaSchedulingFragmentAppointmentsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return NablaSchedulingFragmentAppointmentsBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()

        binding.nablaBookAppointmentButton.setOnClickListener {
            startActivity(ScheduleAppointmentActivity.newIntent(requireContext(), sdkNameOrDefault()))
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

    public companion object {
        public fun newInstance(): AppointmentsFragment = newInstance(NablaClient.DEFAULT_NAME)

        public fun newInstance(sdkName: String): AppointmentsFragment {
            return AppointmentsFragment().apply { setSdkName(sdkName) }
        }
    }
}
