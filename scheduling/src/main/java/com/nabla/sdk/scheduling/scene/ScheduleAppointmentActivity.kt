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
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingActivityScheduleAppointmentHostBinding
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsFragment
import kotlinx.datetime.Instant

@NablaInternal
public class ScheduleAppointmentActivity : AppCompatActivity() {

    private val binding by viewBinding(NablaSchedulingActivityScheduleAppointmentHostBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            pushFragment(CategorySelectionFragment.newInstance(intent.requireSdkName()), isFirstScreen = true)
        }
    }

    internal fun goToTimeSlots(categoryId: CategoryId) {
        pushFragment(TimeSlotsFragment.newInstance(categoryId, intent.requireSdkName()))
    }

    internal fun goToConfirmation(
        categoryId: CategoryId,
        providerId: Uuid,
        slot: Instant,
    ) {
        pushFragment(AppointmentConfirmationFragment.newInstance(categoryId, providerId, slot, intent.requireSdkName()))
    }

    private fun pushFragment(fragment: Fragment, isFirstScreen: Boolean = false) {
        supportFragmentManager.commit {
            if (isFirstScreen) {
                setTransition(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
            } else {
                setTransition(TRANSIT_FRAGMENT_OPEN)
                setCustomAnimations(
                    R.anim.nabla_slide_in_right,
                    R.anim.nabla_fade_out,
                    R.anim.nabla_fade_in,
                    R.anim.nabla_slide_out_right,
                )
            }
            replace(binding.fragmentContainer.id, fragment)
            if (!isFirstScreen) addToBackStack(null)
        }
    }

    internal companion object {
        fun newIntent(context: Context, name: String): Intent = Intent(context, ScheduleAppointmentActivity::class.java).apply {
            setSdkName(name)
        }
    }
}
