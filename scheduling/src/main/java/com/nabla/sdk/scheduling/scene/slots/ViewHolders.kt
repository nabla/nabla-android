package com.nabla.sdk.scheduling.scene.slots

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.toJavaDate
import com.nabla.sdk.core.ui.helpers.StringExtension.capitalize
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemDaySlotsBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemLoadingBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemTimeSlotBinding
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.days

private val diffCallback = object : DiffUtil.ItemCallback<TimeSlotsUiItem.DaySlots.Slot>() {
    override fun areItemsTheSame(
        oldItem: TimeSlotsUiItem.DaySlots.Slot,
        newItem: TimeSlotsUiItem.DaySlots.Slot,
    ): Boolean = oldItem.startAt == newItem.startAt

    override fun areContentsTheSame(
        oldItem: TimeSlotsUiItem.DaySlots.Slot,
        newItem: TimeSlotsUiItem.DaySlots.Slot,
    ): Boolean = oldItem == newItem
}

private class SlotAdapter(
    private val onSlotClicked: (Instant) -> Unit,
) : ListAdapter<TimeSlotsUiItem.DaySlots.Slot, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SlotViewHolder(
        NablaSchedulingItemTimeSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        onSlotClicked,
    )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SlotViewHolder).bind(getItem(position))
    }
}

private class SlotViewHolder(
    private val binding: NablaSchedulingItemTimeSlotBinding,
    private val onSlotClicked: (Instant) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    val timeFormatter = SimpleDateFormat(binding.context.getString(R.string.nabla_scheduling_time_format), Locale.getDefault())

    fun bind(slot: TimeSlotsUiItem.DaySlots.Slot) {
        binding.time.text = timeFormatter.format(slot.startAt.toJavaDate())
        binding.time.setOnClickListener { onSlotClicked(slot.startAt) }
        binding.time.isSelected = slot.isSelected
    }
}

internal class DaySlotsViewHolder(
    private val binding: NablaSchedulingItemDaySlotsBinding,
    singleSlotViewPool: RecyclerView.RecycledViewPool,
    private val onDaySlotsClicked: (position: Int) -> Unit,
    onSlotClicked: (Instant) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val slotAdapter = SlotAdapter(onSlotClicked)

    init {
        binding.availabilitiesContainer.apply {
            layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP).apply {
                justifyContent = JustifyContent.FLEX_START
                alignItems = AlignItems.FLEX_START
            }
            adapter = slotAdapter
            setRecycledViewPool(singleSlotViewPool)
        }
    }

    fun bind(daySlots: TimeSlotsUiItem.DaySlots) {
        binding.date.text = formatDate(daySlots.localDate, binding.context)
        binding.root.setOnClickListener {
            onDaySlotsClicked(adapterPosition)
        }
        bindExpansion(daySlots.expansionState)
    }

    fun bindExpansion(expansionState: TimeSlotsUiItem.DaySlots.ExpansionState) {
        when (expansionState) {
            is TimeSlotsUiItem.DaySlots.ExpansionState.Expanded -> expand(expansionState)
            is TimeSlotsUiItem.DaySlots.ExpansionState.Collapsed -> collapse(expansionState)
        }
    }

    private fun collapse(collapsedState: TimeSlotsUiItem.DaySlots.ExpansionState.Collapsed) {
        val arrowDrawable = ContextCompat.getDrawable(binding.context, R.drawable.nabla_ic_arrow_bottom)
        binding.date.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null)
        binding.availabilitiesCount.isVisible = true
        binding.availabilitiesCount.text = binding.context.resources.getQuantityString(
            R.plurals.nabla_scheduling_time_slot_count,
            collapsedState.slotsCount,
            collapsedState.slotsCount,
        )
        slotAdapter.submitList(emptyList())
    }

    private fun expand(expandedState: TimeSlotsUiItem.DaySlots.ExpansionState.Expanded) {
        val arrowDrawable = ContextCompat.getDrawable(binding.context, R.drawable.nabla_ic_arrow_up)
        binding.date.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null)
        binding.availabilitiesCount.isVisible = false
        slotAdapter.submitList(expandedState.slots)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            singleSlotViewPool: RecyclerView.RecycledViewPool,
            onDaySlotsClicked: (position: Int) -> Unit,
            onSlotClicked: (Instant) -> Unit,
        ): DaySlotsViewHolder {
            return DaySlotsViewHolder(
                NablaSchedulingItemDaySlotsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                singleSlotViewPool,
                onDaySlotsClicked,
                onSlotClicked,
            )
        }
    }
}

internal class LoadingViewHolder(
    val binding: NablaSchedulingItemLoadingBinding,
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(
            parent: ViewGroup,
        ): LoadingViewHolder {
            return LoadingViewHolder(
                NablaSchedulingItemLoadingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}

internal fun formatDate(localDate: LocalDate, context: Context): String {
    val relativeDay = DateUtils.getRelativeTimeSpanString(
        localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        Clock.System.now().toEpochMilliseconds(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_SHOW_DATE
    ).toString()
    val date = DateUtils.formatDateTime(
        context,
        localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE
    )
    return if (localDate.atStartOfDayIn(TimeZone.currentSystemDefault()) - Clock.System.now() > 1.days) {
        date.capitalize()
    } else {
        "$relativeDay ($date)"
    }
}
