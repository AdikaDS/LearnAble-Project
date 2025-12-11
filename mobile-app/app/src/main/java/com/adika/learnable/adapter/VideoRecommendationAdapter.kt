package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.model.VideoRecommendation
import com.adika.learnable.util.VideoThumbnailUtils

class VideoRecommendationAdapter(
    private val onClick: (VideoRecommendation) -> Unit
) : ListAdapter<VideoRecommendation, VideoRecommendationAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VideoRecommendation>() {
            override fun areItemsTheSame(
                oldItem: VideoRecommendation,
                newItem: VideoRecommendation
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: VideoRecommendation,
                newItem: VideoRecommendation
            ): Boolean = oldItem == newItem
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)

        fun bind(item: VideoRecommendation) {

            ivThumbnail.setImageResource(item.thumbnailRes)
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            itemView.setOnClickListener { onClick(item) }

            VideoThumbnailUtils.loadVideoThumbnail(
                target = ivThumbnail,
                videoUrl = item.videoUrl,
                placeholderRes = item.thumbnailRes,
                timeInMs = 1000L,
                width = 320,
                height = 240
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_recommendation, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}