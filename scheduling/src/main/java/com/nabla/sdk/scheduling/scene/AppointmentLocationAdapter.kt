package com.nabla.sdk.scheduling.scene

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemAppointmentLocationBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation

internal class AppointmentLocationAdapter(
    private val onClickAppointmentLocationListener: (AppointmentLocation) -> Unit
) : ListAdapter<AppointmentLocation, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AppointmentLocationViewHolder.create(parent, onClickAppointmentLocationListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AppointmentLocationViewHolder).bind(getItem(position))
    }
}

private class AppointmentLocationViewHolder(
    val binding: NablaSchedulingItemAppointmentLocationBinding,
    val onClickAppointmentLocationListener: (AppointmentLocation) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(location: AppointmentLocation) {
        binding.name.text = binding.context.getString(
            when (location) {
                AppointmentLocation.PHYSICAL -> R.string.nabla_scheduling_location_physical
                AppointmentLocation.REMOTE -> R.string.nabla_scheduling_location_remote
            }
        )
        val startIconRes = when (location) {
            AppointmentLocation.PHYSICAL -> R.drawable.nabla_ic_home_24
            AppointmentLocation.REMOTE -> R.drawable.nabla_ic_video_24
        }
        val endIconRes = R.drawable.nabla_ic_chevron_24
        binding.name.setCompoundDrawablesRelativeWithIntrinsicBounds(startIconRes, 0, endIconRes, 0)
        binding.root.setOnClickListener {
            onClickAppointmentLocationListener(location)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClickAppointmentLocationListener: (AppointmentLocation) -> Unit,
        ): AppointmentLocationViewHolder {
            return AppointmentLocationViewHolder(
                NablaSchedulingItemAppointmentLocationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onClickAppointmentLocationListener
            )
        }
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<AppointmentLocation>() {
    override fun areItemsTheSame(
        oldItem: AppointmentLocation,
        newItem: AppointmentLocation
    ): Boolean = oldItem == newItem

    override fun areContentsTheSame(
        oldItem: AppointmentLocation,
        newItem: AppointmentLocation
    ): Boolean = oldItem == newItem
}
