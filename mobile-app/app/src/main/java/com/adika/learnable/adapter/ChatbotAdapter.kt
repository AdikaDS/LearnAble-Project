package com.adika.learnable.ui

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.model.Chip
import com.google.android.flexbox.FlexboxLayout

class ChatbotAdapter(
    private val messages: MutableList<ChatMessage>,
    private val onChipClick: (Chip) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_USER = 0
    private val TYPE_BOT = 1
    private val TYPE_CHIP = 2

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.UserMessage -> TYPE_USER
            is ChatMessage.BotMessage -> TYPE_BOT
            is ChatMessage.BotChips -> TYPE_CHIP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_user_message, parent, false)
                UserViewHolder(view)
            }
            TYPE_BOT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bot_message, parent, false)
                BotViewHolder(view)
            }
            TYPE_CHIP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chip_list, parent, false)
                ChipViewHolder(view, onChipClick)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is ChatMessage.UserMessage -> (holder as UserViewHolder).bind(message)
            is ChatMessage.BotMessage -> (holder as BotViewHolder).bind(message)
            is ChatMessage.BotChips -> (holder as ChipViewHolder).bind(message)
        }
    }

    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessage.UserMessage) {
            itemView.findViewById<TextView>(R.id.textUser).text = message.text
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessage.BotMessage) {
            itemView.findViewById<TextView>(R.id.textBot).text = message.text
        }
    }

    class ChipViewHolder(itemView: View, val onClick: (Chip) -> Unit) :
        RecyclerView.ViewHolder(itemView) {

        private val chipContainer = itemView.findViewById<FlexboxLayout>(R.id.chipContainer)

        fun bind(message: ChatMessage.BotChips) {
            chipContainer.removeAllViews()
            message.chips.forEach { chipObj ->
                val chip = Button(itemView.context).apply {
                    this.text = chipObj.text
                    setPadding(24, 10, 24, 10)
                    setBackgroundResource(R.drawable.bg_chip)
                    setOnClickListener { onClick(chipObj) }
                }

                val layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(12, 12, 12, 12)
                }
                chip.layoutParams = layoutParams

                chipContainer.addView(chip)
            }
        }
    }
}

sealed class ChatMessage {
    data class UserMessage(val text: String) : ChatMessage()
    data class BotMessage(val text: String) : ChatMessage()
    data class BotChips(val chips: List<Chip>) : ChatMessage()
}
