package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemTeacherSubBabBinding
import com.adika.learnable.model.SubBab

class TeacherSubBabAdapter(
    private val onEditClick: (SubBab) -> Unit,
    private val onDeleteClick: (SubBab) -> Unit
) : ListAdapter<SubBab, TeacherSubBabAdapter.TeacherSubBabViewHolder>(SubBabDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherSubBabViewHolder {
        val binding = ItemTeacherSubBabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeacherSubBabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherSubBabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TeacherSubBabViewHolder(
        private val binding: ItemTeacherSubBabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.editButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }

            binding.videoButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    val videoUrl = subBab.mediaUrls["video"]
                    if (!videoUrl.isNullOrEmpty()) {
                        // TODO: Handle video URL click
                    }
                }
            }

            binding.audioButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    val audioUrl = subBab.mediaUrls["audio"]
                    if (!audioUrl.isNullOrEmpty()) {
                        // TODO: Handle audio URL click
                    }
                }
            }

            binding.pdfButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    val pdfUrl = subBab.mediaUrls["pdfLesson"]
                    if (!pdfUrl.isNullOrEmpty()) {
                        // TODO: Handle PDF URL click
                    }
                }
            }
        }

        fun bind(subBab: SubBab) {
            binding.apply {
                titleText.text = subBab.title
                contentText.text = subBab.content
                durationText.text = itemView.context.getString(R.string.duration_learning, subBab.duration)

                // Enable/disable media buttons based on URL availability
                videoButton.isEnabled = !subBab.mediaUrls["video"].isNullOrEmpty()
                audioButton.isEnabled = !subBab.mediaUrls["audio"].isNullOrEmpty()
                pdfButton.isEnabled = !subBab.mediaUrls["pdfLesson"].isNullOrEmpty()
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