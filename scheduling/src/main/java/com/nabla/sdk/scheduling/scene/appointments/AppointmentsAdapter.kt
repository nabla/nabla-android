package com.nabla.sdk.scheduling.scene.appointments

import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus

internal class AppointmentsAdapter(
    private val onCancelClicked: (appointment: ItemUiModel.AppointmentUiModel.Upcoming) -> Unit,
    private val onJoinClicked: (room: VideoCallRoom, roomStatus: VideoCallRoomStatus.Open) -> Unit,
) : ListAdapter<ItemUiModel, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<ItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ItemUiModel,
            newItem: ItemUiModel,
        ) = oldItem.listId == newItem.listId

        override fun areContentsTheSame(
            oldItem: ItemUiModel,
            newItem: ItemUiModel,
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.LOADING_MORE -> LoadingMoreViewHolder.create(parent)
            ViewType.APPOINTMENT_ITEM -> AppointmentViewHolder.create(parent, onCancelClicked, onJoinClicked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ItemUiModel.AppointmentUiModel -> ViewType.APPOINTMENT_ITEM.ordinal
            is ItemUiModel.Loading -> ViewType.LOADING_MORE.ordinal
        }
    }

    private var avatarRandomSeedOverridden = false
    private var overriddenAvatarRandomSeed: Any? = null

    @VisibleForTesting
    fun overrideAvatarBackgroundRandomSeed(seed: Any?) {
        avatarRandomSeedOverridden = true
        overriddenAvatarRandomSeed = seed
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ItemUiModel.AppointmentUiModel -> (holder as AppointmentViewHolder).apply {
                if (avatarRandomSeedOverridden) {
                    overrideAvatarBackgroundRandomSeed(overriddenAvatarRandomSeed)
                }

                bind(item)
            }
            is ItemUiModel.Loading -> Unit /* no-op */
        }
    }

    private enum class ViewType {
        LOADING_MORE,
        APPOINTMENT_ITEM,
    }
}
