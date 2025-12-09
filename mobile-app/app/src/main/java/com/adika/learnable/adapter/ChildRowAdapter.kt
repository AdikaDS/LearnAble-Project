package com.adika.learnable.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemChildRowBinding
import com.adika.learnable.model.User

class ChildRowAdapter(
    private val onQueryChanged: (position: Int, query: String) -> Unit,
    private val onSelectStudent: (position: Int, student: User) -> Unit,
    private val onRemoveRow: (position: Int) -> Unit,
    private val provideAdapter: (position: Int, onClick: (User) -> Unit) -> StudentSearchAdapter
) : RecyclerView.Adapter<ChildRowAdapter.RowViewHolder>() {

    private val queries = mutableListOf("")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemChildRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowViewHolder(binding)
    }

    override fun getItemCount(): Int = queries.size

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(position)
    }

    fun removeRow(position: Int) {
        if (position in queries.indices) {
            queries.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount - position)
        }
    }

    inner class RowViewHolder(val binding: ItemChildRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private var watcher: TextWatcher? = null
        private var adapter: StudentSearchAdapter? = null

        fun bind(position: Int) {
            binding.etChildName.setText(queries[position])

            adapter = provideAdapter(layoutPosition) { user ->
                binding.etChildName.setText(user.name)
                onSelectStudent(layoutPosition, user)
                binding.rvChildSuggestions.visibility = View.GONE
            }.also { searchAdapter ->
                binding.rvChildSuggestions.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvChildSuggestions.adapter = searchAdapter
            }

            watcher?.let { binding.etChildName.removeTextChangedListener(it) }
            watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val q = s?.toString() ?: ""
                    queries[layoutPosition] = q
                    binding.rvChildSuggestions.visibility = if (q.isBlank()) View.GONE else View.VISIBLE
                    onQueryChanged(layoutPosition, q)
                }
            }
            binding.etChildName.addTextChangedListener(watcher)

            binding.btnRemoveRow.setOnClickListener {
                onRemoveRow(layoutPosition)
            }
        }
    }
}


