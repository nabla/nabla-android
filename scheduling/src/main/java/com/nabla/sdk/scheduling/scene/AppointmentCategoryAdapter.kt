package com.nabla.sdk.scheduling.scene

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nabla.sdk.scheduling.databinding.NablaSchedulingItemAppointmentCategoryBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory

internal class AppointmentCategoryAdapter(
    private val onClickAppointmentCategoryListener: (AppointmentCategory) -> Unit
) : ListAdapter<AppointmentCategory, RecyclerView.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AppointmentCategoryViewHolder.create(parent, onClickAppointmentCategoryListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AppointmentCategoryViewHolder).bind(getItem(position))
    }
}

private class AppointmentCategoryViewHolder(
    val binding: NablaSchedulingItemAppointmentCategoryBinding,
    val onClickAppointmentCategoryListener: (AppointmentCategory) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(category: AppointmentCategory) {
        binding.name.text = category.name
        binding.root.setOnClickListener {
            onClickAppointmentCategoryListener(category)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClickAppointmentCategoryListener: (AppointmentCategory) -> Unit,
        ): AppointmentCategoryViewHolder {
            return AppointmentCategoryViewHolder(
                NablaSchedulingItemAppointmentCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                onClickAppointmentCategoryListener
            )
        }
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<AppointmentCategory>() {
    override fun areItemsTheSame(
        oldItem: AppointmentCategory,
        newItem: AppointmentCategory
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: AppointmentCategory,
        newItem: AppointmentCategory
    ): Boolean = oldItem == newItem
}
