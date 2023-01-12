package com.nabla.sdk.scheduling.scene

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.fragment.app.commit
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.ui.helpers.requireSdkName
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingActivityScheduleAppointmentHostBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsFragment
import kotlinx.datetime.Instant
import com.nabla.sdk.core.R as CoreR

@NablaInternal
public class ScheduleAppointmentActivity : AppCompatActivity() {

    private val binding by viewBinding(NablaSchedulingActivityScheduleAppointmentHostBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            pushFragment(LocationSelectionFragment.newInstance(intent.requireSdkName()), isFirstScreen = true)
        }
    }

    internal fun goToCategorySelection(appointmentLocation: AppointmentLocation, singleLocation: Boolean = false) {
        pushFragment(
            CategorySelectionFragment.newInstance(location = appointmentLocation, showLocationHint = singleLocation, intent.requireSdkName()),
            popBackStack = singleLocation,
            addToBackStack = !singleLocation
        )
    }

    internal fun goToTimeSlots(location: AppointmentLocation, categoryId: CategoryId) {
        pushFragment(
            TimeSlotsFragment.newInstance(location, categoryId, intent.requireSdkName()),
            addToBackStack = true
        )
    }

    internal fun goToConfirmation(
        location: AppointmentLocation,
        categoryId: CategoryId,
        providerId: Uuid,
        slot: Instant,
    ) {
        pushFragment(
            AppointmentConfirmationFragment.newInstance(
                location = location,
                categoryId = categoryId,
                providerId = providerId,
                slot = slot,
                sdkName = intent.requireSdkName()
            ),
            addToBackStack = true
        )
    }

    private fun pushFragment(
        fragment: Fragment,
        isFirstScreen: Boolean = false,
        addToBackStack: Boolean = false,
        popBackStack: Boolean = false
    ) {
        if (popBackStack) {
            supportFragmentManager.popBackStack()
        }
        supportFragmentManager.commit {
            if (isFirstScreen) {
                setTransition(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
            } else {
                setTransition(TRANSIT_FRAGMENT_OPEN)
                setCustomAnimations(
                    CoreR.anim.nabla_slide_in_right,
                    CoreR.anim.nabla_fade_out,
                    CoreR.anim.nabla_fade_in,
                    CoreR.anim.nabla_slide_out_right,
                )
            }
            replace(binding.fragmentContainer.id, fragment)
            if (addToBackStack) addToBackStack(null)
        }
    }

    internal companion object {
        fun newIntent(context: Context, name: String): Intent = Intent(context, ScheduleAppointmentActivity::class.java).apply {
            setSdkName(name)
        }
    }
}
