package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemSubBabBinding
import com.adika.learnable.model.SubBab

class StudentSubBabAdapter(
    private val onSubBabClick: (SubBab) -> Unit
) : ListAdapter<SubBab, StudentSubBabAdapter.StudentSubBabViewHolder>(SubBabDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentSubBabViewHolder {
        val binding = ItemSubBabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentSubBabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentSubBabViewHolder, position: Int) {
        val subBab = getItem(position)
        holder.bind(subBab)
    }

    inner class StudentSubBabViewHolder(
        private val binding: ItemSubBabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    Log.d("SubBabAdapter", "Sub-bab clicked: ${subBab.title}")
                    onSubBabClick(subBab)
                }
            }
        }

        fun bind(subBab: SubBab) {
            binding.apply {
                subBabTitleText.text = subBab.title
                subBabContentText.text = subBab.content
                subBabDurationText.text = itemView.context.getString(R.string.duration_learning, subBab.duration)
            }
        }
    }

    private class SubBabDiffCallback : DiffUtil.ItemCallback<SubBab>() {
        override fun areItemsTheSame(oldItem: SubBab, newItem: SubBab): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SubBab, newItem: SubBab): Boolean {
            return oldItem == newItem
        }
    }
} 