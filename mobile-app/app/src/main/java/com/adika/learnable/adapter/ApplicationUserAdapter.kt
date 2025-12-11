package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemAccountApplicationBinding
import com.adika.learnable.model.User
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.NormalizeFirestore
import com.adika.learnable.util.TextScaleUtils
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicationUserAdapter(
    private val onDetailClick: (User) -> Unit
) : ListAdapter<User, ApplicationUserAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.id == new.id
            override fun areContentsTheSame(old: User, new: User) = old == new
        }

        private val dateFmt = SimpleDateFormat("dd/MM/yyyy - HH.mm", Locale("id"))
    }

    inner class VH(val b: ItemAccountApplicationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: User) = with(b) {
            val normalizedRole =
                item.role?.let { NormalizeFirestore.unormalizeRole(b.root.context, it) }
            tvRole.text = normalizedRole
            tvName.text = item.name
            tvEmail.text = item.email

            tvCreatedAt.text = dateFmt.format(item.createdAt.toDate())

            if (item.isApproved) {
                badgeStatus.text = root.context.getString(R.string.approved)
                badgeStatus.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        R.color.green
                    )
                )
                badgeStatus.setBackgroundResource(R.drawable.bg_badge_approved)
                badgeStatus.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_dot_green),
                    null, null, null
                )
            } else {
                badgeStatus.text = root.context.getString(R.string.rejected)
                badgeStatus.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        R.color.error
                    )
                )
                badgeStatus.setBackgroundResource(R.drawable.bg_badge_unapproved)
                badgeStatus.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(root.context, R.drawable.ic_dot_red),
                    null, null, null
                )
            }

            btnDetail.setOnClickListener { onDetailClick(item) }

            val textScaleRepository = TextScaleRepository(b.root.context)
            val scale = textScaleRepository.getScale()
            TextScaleUtils.applyScaleToHierarchy(b.root, scale)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        val binding = ItemAccountApplicationBinding.inflate(inf, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
