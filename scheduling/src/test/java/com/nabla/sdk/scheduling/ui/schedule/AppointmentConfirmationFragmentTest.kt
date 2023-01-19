package com.nabla.sdk.scheduling.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentConfirmationBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemConsentBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.scene.AppointmentConfirmationFragment.Companion.setHtml
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test

class AppointmentConfirmationFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test loaded consents with confirm button disabled`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.nablaConfirmLoadedGroup.isVisible = true
            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false
            binding.nablaConfirmAppointmentButton.isEnabled = false

            binding.bindConsents(
                consents = consents,
                continueEnabled = false,
                layoutInflater = layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loaded consents with confirm button enabled`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.nablaConfirmLoadedGroup.isVisible = true
            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false

            binding.bindConsents(
                consents = consents,
                continueEnabled = true,
                layoutInflater = layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loading state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.nablaConfirmLoadedGroup.isVisible = false
            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = true
            binding.nablaConfirmAppointmentButton.isEnabled = false

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test error state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.nablaConfirmLoadedGroup.isVisible = false
            binding.errorLayout.root.isVisible = true
            binding.progressBar.isVisible = false
            binding.nablaConfirmAppointmentButton.isEnabled = false

            binding.errorLayout.bind(
                error = ErrorUiModel.Generic,
                onRetryListener = {},
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun NablaSchedulingFragmentAppointmentConfirmationBinding.bindConsents(
        consents: AppointmentConfirmationConsents,
        continueEnabled: Boolean,
        layoutInflater: LayoutInflater,
    ) {
        nablaConfirmAppointmentButton.isEnabled = continueEnabled

        nablaConfirmAppointmentSummary.bind(
            locationType = AppointmentLocationType.REMOTE,
            provider = provider,
            slot = Instant.parse("2028-01-01T10:20:00Z"),
            address = null
        )

        consents.htmlConsents.forEachIndexed { _, html ->
            val consentView = NablaSchedulingItemConsentBinding.inflate(
                layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
                nablaConsentsContainer,
                false
            )
            consentView.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = context.dpToPx(8)
                marginEnd = context.dpToPx(16)
            }
            setHtml(consentView, html)
            nablaConsentsContainer.addView(consentView.root)
            consentView.nablaConsentCheckbox.isChecked = continueEnabled
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentAppointmentConfirmationBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        val binding = NablaSchedulingFragmentAppointmentConfirmationBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false
        )

        parent.addView(binding.root)

        return Pair(parent, binding)
    }

    private companion object {
        val provider = Provider(
            id = Uuid.randomUUID(),
            avatar = null,
            firstName = "Mario",
            lastName = "Bros",
            prefix = "Sir",
            title = "Dr",
        )
        val consents = AppointmentConfirmationConsents(
            htmlConsents = listOf(
                "<p>This is an html string with a <a href='https://www.google.com'>link</a>.</p>",
                "<p>This is a consent that is normal, without any html, but just long enough to be more than 1 line long.</p>",
            )
        )
    }
}
