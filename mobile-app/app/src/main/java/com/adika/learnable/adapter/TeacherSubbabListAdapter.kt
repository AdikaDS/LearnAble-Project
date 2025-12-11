package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemTeacherSubbabListBinding
import com.adika.learnable.model.SubBab

class TeacherSubbabListAdapter(
    private val onDetailClick: (SubBab) -> Unit,
    private val onEditClick: (SubBab) -> Unit,
    private val onDeleteClick: (SubBab) -> Unit
) : ListAdapter<SubBab, TeacherSubbabListAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<SubBab>() {
        override fun areItemsTheSame(oldItem: SubBab, newItem: SubBab): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SubBab, newItem: SubBab): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTeacherSubbabListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTeacherSubbabListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subBab: SubBab) {
            binding.apply {
                tvSubbabName.text = subBab.title

                btnDetail.setOnClickListener {
                    onDetailClick(subBab)
                }

                btnEdit.setOnClickListener {
                    onEditClick(subBab)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(subBab)
                }
            }
        }
    }
}