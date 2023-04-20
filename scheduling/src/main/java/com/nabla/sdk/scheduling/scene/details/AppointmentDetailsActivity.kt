package com.nabla.sdk.scheduling.scene.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.ui.helpers.SceneHelpers.requireSdkName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingActivityAppointmentDetailsHostBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.scene.requireAppointmentId
import com.nabla.sdk.scheduling.scene.setAppointmentId

public class AppointmentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: NablaSchedulingActivityAppointmentDetailsHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NablaSchedulingActivityAppointmentDetailsHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    R.id.fragmentContainer,
                    AppointmentDetailsFragment.newInstance(
                        intent.requireAppointmentId(),
                        intent.requireSdkName(),
                    ),
                )
            }
        }
    }

    public companion object {
        public fun newIntent(
            context: Context,
            appointmentId: AppointmentId,
            sdkName: String = NablaClient.DEFAULT_NAME,
        ): Intent = Intent(context, AppointmentDetailsActivity::class.java).apply {
            setAppointmentId(appointmentId)
            setSdkName(sdkName)
        }
    }
}
