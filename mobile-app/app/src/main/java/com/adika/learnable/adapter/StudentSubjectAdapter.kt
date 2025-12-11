package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemSubjectBinding
import com.adika.learnable.model.Subject
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.SubjectIconUtils.setSubjectIcon
import com.adika.learnable.util.TextScaleUtils

class StudentSubjectAdapter(
    private val onSubjectClick: (Subject) -> Unit
) : ListAdapter<Subject, StudentSubjectAdapter.SubjectViewHolder>(SubjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubjectViewHolder(
        private val binding: ItemSubjectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSubjectClick(getItem(position))
                }
            }
        }

        fun bind(subject: Subject) {
            binding.apply {
                tvSubjectName.text = subject.name
                ivSubject.setSubjectIcon(subject.name)
                ivSubject.post {
                    val imageHeight = ivSubject.height
                    val overlayHeight = imageHeight / 2

                    val params = gradientOverlay.layoutParams
                    params.height = overlayHeight
                    gradientOverlay.layoutParams = params
                }
                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }
    }

    private class SubjectDiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean {
            return oldItem.idSubject == newItem.idSubject
        }

        override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean {
            return oldItem == newItem
        }
    }
} 