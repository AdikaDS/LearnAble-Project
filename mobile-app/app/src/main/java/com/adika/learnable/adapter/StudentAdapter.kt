package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemStudentBinding
import com.adika.learnable.model.User
import com.bumptech.glide.Glide

class StudentAdapter(
    private val onStudentClick: (User) -> Unit
) : ListAdapter<User, StudentAdapter.StudentViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StudentViewHolder(
        private val binding: ItemStudentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStudentClick(getItem(position))
                }
            }
        }

        fun bind(student: User) {
            binding.apply {
                tvStudentName.text = student.name
                tvStudentEmail.text = student.email

                // Load profile picture if available
                student.profilePicture.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .placeholder(R.drawable.ic_user)
                        .into(ivStudentProfile)
                }
            }
        }
    }

    private class StudentDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
} 