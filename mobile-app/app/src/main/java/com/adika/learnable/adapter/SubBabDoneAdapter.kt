package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemDoneLearningBinding
import com.adika.learnable.model.SubBabDoneItem
import com.bumptech.glide.Glide

class SubBabDoneAdapter :
    ListAdapter<SubBabDoneItem, SubBabDoneAdapter.SubBabDoneViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubBabDoneViewHolder {
        val binding = ItemDoneLearningBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubBabDoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubBabDoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubBabDoneViewHolder(
        private val binding: ItemDoneLearningBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubBabDoneItem) {
            val ctx = itemView.context

            binding.apply {
                tvTitle.text = item.title
                titleText.text = item.subtitle
                progressText.text = ctx.getString(R.string.done)

                item.coverImage.takeIf { it.isNotBlank() }?.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(imageView)
                } ?: run {
                    imageView.setImageResource(R.drawable.icon_dummy_bab)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SubBabDoneItem>() {
        override fun areItemsTheSame(oldItem: SubBabDoneItem, newItem: SubBabDoneItem): Boolean {
            val oldKey = oldItem.progress.lessonId + "#" + oldItem.progress.subBabId
            val newKey = newItem.progress.lessonId + "#" + newItem.progress.subBabId
            return oldKey == newKey
        }

        override fun areContentsTheSame(oldItem: SubBabDoneItem, newItem: SubBabDoneItem): Boolean {
            return oldItem == newItem
        }
    }
}