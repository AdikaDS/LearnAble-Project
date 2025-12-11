package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemProgressHorizontalBinding
import com.adika.learnable.model.SubBabProgressHorizontalItem
import com.bumptech.glide.Glide

class HorizontalSubBabProgressAdapter(
) : ListAdapter<SubBabProgressHorizontalItem, HorizontalSubBabProgressAdapter.HorizontalSubBabProgressViewHolder>(
    DiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HorizontalSubBabProgressViewHolder {
        val binding = ItemProgressHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HorizontalSubBabProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HorizontalSubBabProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HorizontalSubBabProgressViewHolder(
        private val binding: ItemProgressHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubBabProgressHorizontalItem) {
            val ctx = itemView.context
            binding.apply {
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle
                tvGradeBadge.text = item.schoolLevel

                val completedMaterials = item.progress.completedMaterials.values.count { it }
                val totalMaterials = item.progress.completedMaterials.size
                val progressPercentages = if (totalMaterials > 0) {
                    (completedMaterials * 100) / totalMaterials
                } else 0

                progressText.text =
                    ctx.getString(R.string.progress_text, completedMaterials, totalMaterials)

                progressBar.progress = progressPercentages

                val colorRes = if (completedMaterials == totalMaterials && totalMaterials > 0) {
                    R.color.green
                } else {
                    R.color.blue_primary
                }
                progressBar.setIndicatorColor(ctx.getColor(colorRes))

                item.coverImage.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(ivIcon)
                }

                ivIcon.post {
                    val imageHeight = ivIcon.height
                    val overlayHeight = imageHeight / 2

                    val params = gradientOverlay.layoutParams
                    params.height = overlayHeight
                    gradientOverlay.layoutParams = params
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SubBabProgressHorizontalItem>() {
        override fun areItemsTheSame(
            oldItem: SubBabProgressHorizontalItem,
            newItem: SubBabProgressHorizontalItem
        ): Boolean {
            return oldItem.progress.subBabId == newItem.progress.subBabId
        }

        override fun areContentsTheSame(
            oldItem: SubBabProgressHorizontalItem,
            newItem: SubBabProgressHorizontalItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}