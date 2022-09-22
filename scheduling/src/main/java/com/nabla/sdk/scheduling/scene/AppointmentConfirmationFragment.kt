package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal
import com.nabla.sdk.core.ui.helpers.context
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
                requireArguments().getCategoryId(),
                requireArguments().getProviderId(),
                requireArguments().getSlot(),
                getNablaInstanceByName(),
            )
        }
    }

    private lateinit var dateFormatter: SimpleDateFormat

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dateFormatter = SimpleDateFormat(getString(R.string.nabla_scheduling_time_format), Locale.getDefault())

        binding.toolbar.setNavigationOnClickListener { hostActivity().onBackPressed() }
        binding.nablaConfirmAppointmentButton.setOnClickListener { viewModel.onConfirmClicked() }
        binding.errorLayout.nablaErrorRetryButton.setOnClickListener { viewModel.onClickRetry() }
        binding.nablaConfirmCheckbox1.setOnCheckedChangeListener { _, checked -> viewModel.onFirstConsentChecked(checked) }
        binding.nablaConfirmCheckbox2.setOnCheckedChangeListener { _, checked -> viewModel.onSecondConsentChecked(checked) }

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
                    Toast.makeText(context, event.errorMessage, Toast.LENGTH_LONG).show()
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
            getString(R.string.nabla_scheduling_date_pill_format_today, formattedTime)
        } else {
            val formattedDate = SimpleDateFormat(getString(R.string.nabla_scheduling_date_pill_format_date), Locale.getDefault())
                .format(toJavaDate())
            getString(R.string.nabla_scheduling_date_pill_format_future, formattedDate, formattedTime)
        }
    }

    internal companion object {
        private const val ARG_CATEGORY_ID = "ARG_CATEGORY_ID"
        private const val ARG_PROVIDER_ID = "ARG_PROVIDER_ID"
        private const val ARG_SLOT_INSTANT = "ARG_SLOT_INSTANT"

        internal fun newInstance(
            categoryId: CategoryId,
            providerId: Uuid,
            slot: Instant,
            sdkName: String,
        ) = AppointmentConfirmationFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY_ID, categoryId.value.toString())
                putString(ARG_PROVIDER_ID, providerId.toString())
                putLong(ARG_SLOT_INSTANT, slot.toEpochMilliseconds())
            }
            setSdkName(sdkName)
        }

        private fun Bundle.getCategoryId() = CategoryId(
            Uuid.fromString(
                getString(ARG_CATEGORY_ID) ?: throw IllegalStateException("Missing Category Id").asNablaInternal()
            )
        )

        private fun Bundle.getProviderId() =
            Uuid.fromString(
                getString(ARG_PROVIDER_ID) ?: throw IllegalStateException("Missing Provider Id").asNablaInternal()
            )

        private fun Bundle.getSlot() = Instant.fromEpochMilliseconds(
            getLong(ARG_SLOT_INSTANT).also { if (it == 0L) throw IllegalStateException("Missing Slot Instant").asNablaInternal() }

        )
    }
}
