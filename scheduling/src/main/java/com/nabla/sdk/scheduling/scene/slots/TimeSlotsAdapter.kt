package com.nabla.sdk.scheduling.scene.slots

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import kotlinx.datetime.Instant

internal class TimeSlotsAdapter(
    private val onDaySlotsClicked: (position: Int) -> Unit,
    private val onSlotClicked: (Instant) -> Unit,
    private val onBindLoading: () -> Unit,
) : ListAdapter<TimeSlotsUiItem, RecyclerView.ViewHolder>(diffCallback) {
    private val singleSlotViewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.DAY_SLOTS -> DaySlotsViewHolder.create(parent, singleSlotViewPool, onDaySlotsClicked, onSlotClicked)
            ViewType.LOADING -> LoadingViewHolder.create(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TimeSlotsUiItem.DaySlots -> {
                (holder as DaySlotsViewHolder).bind(item)
            }
            is TimeSlotsUiItem.Loading -> onBindLoading()
            else -> { /* no-op */
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        }
        payloads.forEach {
            when (val payload = it as Payload) {
                is Payload.ExpansionState -> (holder as DaySlotsViewHolder).bindExpansion(payload.expansionState)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TimeSlotsUiItem.DaySlots -> ViewType.DAY_SLOTS.ordinal
            is TimeSlotsUiItem.Loading -> ViewType.LOADING.ordinal
            else -> throwNablaInternalException("Unknown item type ${getItem(position)}")
        }
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<TimeSlotsUiItem>() {
    override fun areItemsTheSame(
        oldItem: TimeSlotsUiItem,
        newItem: TimeSlotsUiItem,
    ): Boolean = oldItem.listId == newItem.listId

    override fun areContentsTheSame(
        oldItem: TimeSlotsUiItem,
        newItem: TimeSlotsUiItem,
    ): Boolean = oldItem == newItem

    override fun getChangePayload(oldItem: TimeSlotsUiItem, newItem: TimeSlotsUiItem): Any? {
        if (oldItem is TimeSlotsUiItem.DaySlots && newItem is TimeSlotsUiItem.DaySlots) {
            if (oldItem.copy(expansionState = newItem.expansionState) == newItem) {
                // only expansion state changed
                return Payload.ExpansionState(newItem.expansionState)
            }
        }
        return super.getChangePayload(oldItem, newItem)
    }
}

private sealed class Payload {
    data class ExpansionState(val expansionState: TimeSlotsUiItem.DaySlots.ExpansionState) : Payload()
}

private enum class ViewType {
    DAY_SLOTS, LOADING
}
