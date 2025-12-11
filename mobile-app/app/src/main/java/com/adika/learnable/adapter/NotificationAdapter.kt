package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemNotificationBinding
import com.adika.learnable.model.Notification
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onNotificationClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val ctx = itemView.context
            binding.apply {
                textSender.text = ctx.getString(R.string.system)
                textMessage.text = notification.message

                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                textTimestamp.text = dateFormat.format(notification.createdAt.toDate())

                indicatorUnread.visibility =
                    if (notification.isRead) View.INVISIBLE else View.VISIBLE

                root.setOnClickListener {
                    onNotificationClick(notification)
                }

                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}