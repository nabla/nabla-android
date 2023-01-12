package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.domain.entity.evaluate
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.setSdkName
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.core.ui.helpers.viewBinding
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentConfirmationBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemConsentBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.AppointmentConfirmationViewModel.Event
import com.nabla.sdk.scheduling.scene.AppointmentConfirmationViewModel.State
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale

internal class AppointmentConfirmationFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_appointment_confirmation
) {
    private val binding by viewBinding(NablaSchedulingFragmentAppointmentConfirmationBinding::bind)
    private val viewModel: AppointmentConfirmationViewModel by viewModels {
        factoryFor {
            AppointmentConfirmationViewModel(
                requireLocation(),
                requireCategoryId(),
                requireArguments().getProviderId(),
                requireArguments().getSlot(),
                getNablaInstanceByName(),
            )
        }
    }

    private lateinit var dateFormatter: SimpleDateFormat

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val locationIconRes = when (requireLocation()) {
            AppointmentLocation.PHYSICAL -> R.drawable.nabla_ic_home_20
            AppointmentLocation.REMOTE -> R.drawable.nabla_ic_video_20
        }

        binding.nablaConfirmAppointmentDatePill.setCompoundDrawablesRelativeWithIntrinsicBounds(
            locationIconRes,
            0,
            0,
            0
        )

        dateFormatter = SimpleDateFormat(getString(R.string.nabla_scheduling_time_format), Locale.getDefault())

        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }
        binding.nablaConfirmAppointmentButton.setOnClickListener { viewModel.onConfirmClicked() }
        binding.errorLayout.nablaErrorRetryButton.setOnClickListener { viewModel.onClickRetry() }

        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.nablaConfirmLoadedGroup.isVisible = state is State.Loaded
            binding.errorLayout.root.isVisible = state is State.Error
            binding.progressBar.isVisible = state is State.Loading

            when (state) {
                is State.Error -> binding.errorLayout.bind(state.errorUiModel, viewModel::onClickRetry)
                is State.Loaded -> {
                    binding.nablaConfirmAppointmentAvatar.loadAvatar(state.provider)
                    binding.nablaConfirmAppointmentTitle.text = state.provider.fullNameWithPrefix(binding.context)
                    binding.nablaConfirmAppointmentSubtitle.setTextOrHide(state.provider.title)
                    binding.nablaConfirmAppointmentDatePill.text = state.slot.formatScheduledAt()

                    binding.nablaConsentsContainer.removeAllViews()
                    state.consents.htmlConsents.forEachIndexed { index, html ->
                        val consentView = NablaSchedulingItemConsentBinding.inflate(layoutInflater, binding.nablaConsentsContainer, false)
                        consentView.root.updateLayoutParams<MarginLayoutParams> {
                            topMargin = view.context.dpToPx(8)
                            marginEnd = view.context.dpToPx(16)
                        }
                        consentView.nablaConsentCheckbox.setOnCheckedChangeListener { _, checked -> viewModel.onConsentChecked(index, checked) }
                        setHtml(consentView, html)
                        binding.nablaConsentsContainer.addView(consentView.root)
                    }
                }
                is State.Loading -> Unit /* no-op */
            }
        }
        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.canSubmitFlow) { canSubmit ->
            binding.nablaConfirmAppointmentButton.isEnabled = canSubmit
        }

        viewLifecycleOwner.launchCollect(viewModel.eventsFlow) { event ->
            when (event) {
                is Event.FailedSubmitting -> {
                    Toast.makeText(context, event.errorMessage.evaluate(this), Toast.LENGTH_LONG).show()
                }
                Event.Finish -> {
                    hostActivity().finish()
                }
            }
        }
    }

    private fun Instant.formatScheduledAt(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isToday = today == toLocalDateTime(TimeZone.currentSystemDefault()).date

        val formattedTime = dateFormatter.format(toJavaDate())

        return if (isToday) {
            val stringRes = when (requireLocation()) {
                AppointmentLocation.PHYSICAL -> R.string.nabla_scheduling_physical_date_format_today
                AppointmentLocation.REMOTE -> R.string.nabla_scheduling_remote_date_format_today
            }
            getString(stringRes, formattedTime)
        } else {
            val formattedDate = SimpleDateFormat(getString(R.string.nabla_scheduling_date_pill_format_date), Locale.getDefault())
                .format(toJavaDate())
            val stringRes = when (requireLocation()) {
                AppointmentLocation.PHYSICAL -> R.string.nabla_scheduling_physical_date_format_future
                AppointmentLocation.REMOTE -> R.string.nabla_scheduling_remote_date_format_future
            }
            getString(stringRes, formattedDate, formattedTime)
        }
    }

    internal companion object {
        private const val ARG_PROVIDER_ID = "ARG_PROVIDER_ID"
        private const val ARG_SLOT_INSTANT = "ARG_SLOT_INSTANT"

        internal fun newInstance(
            location: AppointmentLocation,
            categoryId: CategoryId,
            providerId: Uuid,
            slot: Instant,
            sdkName: String,
        ) = AppointmentConfirmationFragment().apply {
            arguments = bundleOf(
                ARG_PROVIDER_ID to providerId.toString(),
                ARG_SLOT_INSTANT to slot.toEpochMilliseconds(),
            )
            setLocation(location)
            setCategoryId(categoryId)
            setSdkName(sdkName)
        }

        private fun Bundle.getProviderId() =
            Uuid.fromString(
                getString(ARG_PROVIDER_ID) ?: throwNablaInternalException("Missing Provider Id")
            )

        private fun Bundle.getSlot() = Instant.fromEpochMilliseconds(
            getLong(ARG_SLOT_INSTANT).also { if (it == 0L) throwNablaInternalException("Missing Slot Instant") }

        )

        @VisibleForTesting
        internal fun setHtml(itemConsentBinding: NablaSchedulingItemConsentBinding, html: String) {
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            itemConsentBinding.nablaConsentText.text = spanned

            // Makes hyperlinks clickable
            itemConsentBinding.nablaConsentText.movementMethod = LinkMovementMethod.getInstance()

            // Let text click toggle checkbox if no link
            if (spanned.getSpans<ClickableSpan>().isEmpty()) {
                itemConsentBinding.nablaConsentText.setOnClickListener {
                    itemConsentBinding.nablaConsentCheckbox.toggle()
                }
            }
        }
    }
}
