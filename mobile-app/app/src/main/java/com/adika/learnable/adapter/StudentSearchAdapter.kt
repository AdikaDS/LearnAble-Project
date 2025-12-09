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

class StudentSearchAdapter(
    var onItemClick: (User) -> Unit
) : ListAdapter<User, StudentSearchAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStudentBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = layoutPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
            }
        }

        fun bind(user: User) {
            binding.tvStudentName.text = user.name
            binding.tvStudentEmail.text = user.email
            binding.tvNomorInduk.text = user.nomorInduk
            Glide.with(binding.root.context)
                .load(user.profilePicture)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .into(binding.ivStudentProfile)
        }
    }

    private class Diff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}


