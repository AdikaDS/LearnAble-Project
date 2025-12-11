package com.adika.learnable.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.model.StepItem
import com.adika.learnable.util.VideoThumbnailUtils
import com.google.android.material.card.MaterialCardView

class StepAdapter(
    private val items: MutableList<StepItem>,
    private val onClick: (position: Int, item: StepItem) -> Unit,
    private val videoUrls: Map<String, String> = emptyMap()
) : RecyclerView.Adapter<StepAdapter.VH>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_VIDEO = 1
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val ivStatus: ImageView = view.findViewById(R.id.ivStatus)
        val thumbnailIcon: ImageView = view.findViewById(R.id.thumbnailIcon)
        val playButtonOverlay: ImageView = view.findViewById(R.id.playButtonOverlay)
        val loadingIndicator: ProgressBar? = view.findViewById(R.id.loadingIndicator)
        val lineTop: View = view.findViewById(R.id.lineTop)
        val lineBottom: View = view.findViewById(R.id.lineBottom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutRes = when (viewType) {
            VIEW_TYPE_VIDEO -> R.layout.item_timeline_video
            else -> R.layout.item_timeline
        }
        val v = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return VH(v)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].key) {
            "video" -> VIEW_TYPE_VIDEO
            else -> VIEW_TYPE_NORMAL
        }
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val ctx = h.itemView.context
        val item = items[position]

        h.tvTitle.text = item.title
        h.loadingIndicator?.visibility = View.GONE
        when (item.key) {
            "video" -> {
                // Show loading indicator and hide play button initially
                h.loadingIndicator?.visibility = View.VISIBLE
                h.playButtonOverlay.visibility = View.GONE
                loadVideoThumbnail(
                    h.thumbnailIcon,
                    item.key,
                    h.loadingIndicator,
                    h.playButtonOverlay
                )
            }

            else -> {
                when (item.key) {
                    "pdf" -> h.thumbnailIcon.setImageResource(R.drawable.icon_material)
                    "quiz" -> h.thumbnailIcon.setImageResource(R.drawable.icon_quiz)
                    else -> h.thumbnailIcon.setImageResource(R.drawable.icon_material)
                }
                h.playButtonOverlay.visibility = View.GONE
            }
        }
        if (item.key == "quiz" && !item.scoreText.isNullOrBlank()) {
            h.tvScore.visibility = View.VISIBLE
            h.tvScore.text = item.scoreText
        } else {
            h.tvScore.visibility = View.GONE
            h.tvScore.text = ""
        }

        if (item.isCompleted) {
            h.ivStatus.setImageResource(R.drawable.step_circle_completed)
            h.card.strokeColor = ContextCompat.getColor(ctx, R.color.blue_secondary)
        } else {
            h.ivStatus.setImageResource(R.drawable.step_circle_not_completed)
            h.card.strokeColor = ContextCompat.getColor(ctx, R.color.grey_bg)
        }
        val isFirst = position == 0
        val isLast = position == items.lastIndex
        h.lineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
        h.lineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

        val blue = ContextCompat.getColor(ctx, R.color.blue_secondary)
        val grey = ContextCompat.getColor(ctx, R.color.grey_bg)
        val lineColor = if (item.isCompleted) blue else grey
        h.lineTop.setBackgroundColor(lineColor)
        h.lineBottom.setBackgroundColor(lineColor)

        h.card.setOnClickListener { onClick(position, item) }
    }

    override fun getItemCount() = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<StepItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun loadVideoThumbnail(
        imageView: ImageView,
        key: String,
        loadingIndicator: ProgressBar? = null,
        playButtonOverlay: ImageView? = null
    ) {
        val videoUrl = videoUrls[key]
        loadingIndicator?.visibility = View.VISIBLE
        playButtonOverlay?.visibility = View.GONE

        VideoThumbnailUtils.loadVideoThumbnail(
            target = imageView,
            videoUrl = videoUrl,
            placeholderRes = R.drawable.ic_video_placeholder,
            timeInMs = 1000L,
            width = 320,
            height = 240,
            onLoadComplete = {
                loadingIndicator?.visibility = View.GONE
                playButtonOverlay?.visibility = View.VISIBLE
            }
        )
    }
}