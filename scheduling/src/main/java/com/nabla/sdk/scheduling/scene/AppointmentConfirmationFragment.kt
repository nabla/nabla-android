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
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.evaluate
import com.nabla.sdk.core.ui.helpers.CoroutineScopeExtension.launchCollect
import com.nabla.sdk.core.ui.helpers.DensityExtensions.dpToPx
import com.nabla.sdk.core.ui.helpers.SceneHelpers.getNablaInstanceByName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.core.ui.helpers.ViewModelExtension.savedStateFactoryFor
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentConfirmationBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemConsentBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.scene.AppointmentConfirmationViewModel.Event
import com.nabla.sdk.scheduling.scene.AppointmentConfirmationViewModel.State
import com.nabla.sdk.scheduling.schedulingPrivateClient

internal class AppointmentConfirmationFragment : BookAppointmentBaseFragment(
    R.layout.nabla_scheduling_fragment_appointment_confirmation,
) {
    private val nablaClient = getNablaInstanceByName()
    private val paymentActivityContract = nablaClient.schedulingPrivateClient.paymentActivityContract
    private val binding by viewBinding(NablaSchedulingFragmentAppointmentConfirmationBinding::bind)
    private val viewModel: AppointmentConfirmationViewModel by viewModels {
        savedStateFactoryFor { handle ->
            AppointmentConfirmationViewModel(
                locationType = requireAppointmentLocationType(),
                pendingAppointmentId = requireArguments().getPendingAppointmentIdOrNull(),
                paymentStepRegistered = paymentActivityContract != null,
                nablaClient = nablaClient,
                handle = handle,
            )
        }
    }
    private val paymentLauncher = paymentActivityContract?.let { contract ->
        registerForActivityResult(contract) { succeeded ->
            viewModel.onPaymentFinished(succeeded)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressedDispatcher.onBackPressed() }
        binding.nablaConfirmAppointmentButton.setOnClickListener { viewModel.onCtaClicked() }
        binding.errorLayout.nablaErrorRetryButton.setOnClickListener { viewModel.onClickRetry() }

        viewLifecycleOwner.lifecycleScope.launchCollect(viewModel.stateFlow) { state ->
            binding.nablaConfirmLoadedGroup.isVisible = state is State.Loaded
            binding.errorLayout.root.isVisible = state is State.Error
            binding.progressBar.isVisible = state is State.Loading

            when (state) {
                is State.Error -> binding.errorLayout.bind(state.errorUiModel, viewModel::onClickRetry)
                is State.Loaded -> {
                    binding.nablaConfirmAppointmentSummary.bind(
                        state.appointment.location,
                        state.appointment.provider,
                        state.appointment.scheduledAt,
                        (state.appointment.state as? AppointmentState.Pending)?.requiredPrice,
                    )

                    binding.nablaConfirmAppointmentButton.setText(
                        if (state.requiresPayment) R.string.nabla_scheduling_go_payment_cta else R.string.nabla_scheduling_confirm_cta,
                    )

                    binding.nablaConsentsContainer.removeAllViews()
                    state.htmlConsents.forEachIndexed { index, consent ->
                        val (html, isChecked) = consent
                        val consentView = NablaSchedulingItemConsentBinding.inflate(layoutInflater, binding.nablaConsentsContainer, false)
                        consentView.root.updateLayoutParams<MarginLayoutParams> {
                            marginEnd = view.context.dpToPx(16)
                        }
                        consentView.nablaConsentCheckbox.isChecked = isChecked
                        viewModel.onConsentChecked(index, consentView.nablaConsentCheckbox.isChecked)
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
                is Event.ShowMessage -> {
                    Toast.makeText(context, event.message.evaluate(this), Toast.LENGTH_LONG).show()
                }
                Event.ShowConfirmation -> {
                    hostActivity().goToSuccess()
                }
                is Event.StartPayment -> {
                    paymentLauncher?.launch(event.pendingAppointment)
                        ?: throwNablaInternalException("ViewModel said to StartPayment but no launcher registered")
                }
            }
        }
    }

    internal companion object {
        private const val ARG_PENDING_APPOINTMENT_UUID = "ARG_PENDING_APPOINTMENT_UUID"

        internal fun newInstance(
            locationType: AppointmentLocationType,
            pendingAppointmentId: AppointmentId,
            sdkName: String,
        ) = AppointmentConfirmationFragment().apply {
            arguments = bundleOf(
                ARG_PENDING_APPOINTMENT_UUID to pendingAppointmentId.uuid.toString(),
            )
            setAppointmentLocationType(locationType)
            setSdkName(sdkName)
        }

        private fun Bundle.getPendingAppointmentIdOrNull() = AppointmentId(
            Uuid.fromString(getString(ARG_PENDING_APPOINTMENT_UUID) ?: throwNablaInternalException("Missing pending appointment id")),
        )

        @VisibleForTesting
        internal fun setHtml(itemConsentBinding: NablaSchedulingItemConsentBinding, html: String) {
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            itemConsentBinding.nablaConsentText.text = spanned.trim()

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
