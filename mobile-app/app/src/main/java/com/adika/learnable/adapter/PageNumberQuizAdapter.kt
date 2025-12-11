package com.adika.learnable.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemPageNumberBinding
import com.adika.learnable.model.QuizPage
import com.adika.learnable.model.QuizPageState

class PageNumberQuizAdapter(
    private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PageNumberQuizAdapter.VH>() {

    private val items = mutableListOf<QuizPage>()

    private var selectedIndex = 0
        set(value) {
            val old = field
            field = value
            notifyItemChanged(old)
            notifyItemChanged(value)
        }

    private var isReviewMode = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(val binding: ItemPageNumberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPageNumberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvNumber.text = item.number.toString()

        if (isReviewMode) {

            holder.binding.tvNumber.background = null
            when (item.state) {
                QuizPageState.CORRECT -> {
                    holder.binding.tvNumber.setTextColor(
                        ContextCompat.getColor(holder.binding.root.context, R.color.green)
                    )
                }

                QuizPageState.INCORRECT -> {
                    holder.binding.tvNumber.setTextColor(
                        ContextCompat.getColor(holder.binding.root.context, R.color.error)
                    )
                }

                QuizPageState.ANSWERED -> {
                    holder.binding.tvNumber.setTextColor(
                        ContextCompat.getColor(holder.binding.root.context, R.color.grey)
                    )
                }

                else -> {
                    holder.binding.tvNumber.setTextColor(
                        ContextCompat.getColor(holder.binding.root.context, R.color.text_primary)
                    )
                }
            }
        } else {
            holder.binding.tvNumber.isSelected = (position == selectedIndex)
            holder.binding.tvNumber.isActivated = (item.state == QuizPageState.ANSWERED)
        }
    }

    override fun getItemCount() = items.size

    fun submitCount(count: Int) {
        val newItems = (1..count).map { QuizPage(it) }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].number == newItems[newPos].number

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun markAnswered(index: Int) {
        if (index in items.indices) {
            val newItems = items.toMutableList()
            newItems[index] = newItems[index].copy(state = QuizPageState.ANSWERED)
            updateItems(newItems)
        }
    }

    fun markUnanswered(index: Int) {
        if (index in items.indices) {
            val newItems = items.toMutableList()
            newItems[index] = newItems[index].copy(state = QuizPageState.UNANSWERED)
            updateItems(newItems)
        }
    }

    fun applyResults(correct: Set<Int>, wrong: Set<Int>, pending: Set<Int> = emptySet()) {
        val newItems = items.mapIndexed { i, page ->
            when {
                i in correct -> page.copy(state = QuizPageState.CORRECT)
                i in wrong -> page.copy(state = QuizPageState.INCORRECT)
                i in pending -> page.copy(state = QuizPageState.ANSWERED)
                else -> page
            }
        }
        updateItems(newItems)
        isReviewMode = true
    }

    fun updateSelectedIndex(index: Int) {
        if (index in 0 until items.size && index != selectedIndex) {
            selectedIndex = index
        }
    }

    private fun updateItems(newItems: List<QuizPage>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].number == newItems[newPos].number

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }
}