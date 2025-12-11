package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemWeekHistoryBinding
import com.adika.learnable.model.WeekGroup
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils

class WeeklyHistoryAdapter : ListAdapter<WeekGroup, WeeklyHistoryAdapter.WeekViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WeekGroup>() {
            override fun areItemsTheSame(oldItem: WeekGroup, newItem: WeekGroup): Boolean =
                oldItem.title == newItem.title

            override fun areContentsTheSame(oldItem: WeekGroup, newItem: WeekGroup): Boolean =
                oldItem == newItem
        }
    }

    inner class WeekViewHolder(private val binding: ItemWeekHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val innerAdapter = ProgressHistoryAdapter()

        init {
            binding.rvItems.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvItems.adapter = innerAdapter
            binding.rvItems.setHasFixedSize(false)
            binding.rvItems.isNestedScrollingEnabled = false
        }

        fun bind(group: WeekGroup) {
            binding.tvWeekTitle.text = group.title
            innerAdapter.submitList(group.items)

            val textScaleRepository = TextScaleRepository(binding.root.context)
            val scale = textScaleRepository.getScale()
            TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWeekHistoryBinding.inflate(inflater, parent, false)
        return WeekViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}


