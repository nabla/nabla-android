package com.nabla.sdk.scheduling.scene

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemAppointmentLocationBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType

internal class AppointmentLocationAdapter(
    private val onClickAppointmentLocationTypeListener: (AppointmentLocationType) -> Unit
) : ListAdapter<AppointmentLocationType, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AppointmentLocationViewHolder.create(parent, onClickAppointmentLocationTypeListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AppointmentLocationViewHolder).bind(getItem(position))
    }
}

private class AppointmentLocationViewHolder(
    val binding: NablaSchedulingItemAppointmentLocationBinding,
    val onClickAppointmentLocationListener: (AppointmentLocationType) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(locationType: AppointmentLocationType) {
        binding.name.text = binding.context.getString(
            when (locationType) {
                AppointmentLocationType.PHYSICAL -> R.string.nabla_scheduling_location_physical
                AppointmentLocationType.REMOTE -> R.string.nabla_scheduling_location_remote
            }
        )
        val startIconRes = when (locationType) {
            AppointmentLocationType.PHYSICAL -> R.drawable.nabla_ic_home_24
            AppointmentLocationType.REMOTE -> R.drawable.nabla_ic_video_24
        }
        val endIconRes = R.drawable.nabla_ic_chevron_24
        binding.name.setCompoundDrawablesRelativeWithIntrinsicBounds(startIconRes, 0, endIconRes, 0)
        binding.root.setOnClickListener {
            onClickAppointmentLocationListener(locationType)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClickAppointmentLocationTypeListener: (AppointmentLocationType) -> Unit,
        ): AppointmentLocationViewHolder {
            return AppointmentLocationViewHolder(
                NablaSchedulingItemAppointmentLocationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onClickAppointmentLocationTypeListener
            )
        }
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<AppointmentLocationType>() {
    override fun areItemsTheSame(
        oldItem: AppointmentLocationType,
        newItem: AppointmentLocationType
    ): Boolean = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: AppointmentLocationType,
        newItem: AppointmentLocationType
    ): Boolean = oldItem == newItem
}
