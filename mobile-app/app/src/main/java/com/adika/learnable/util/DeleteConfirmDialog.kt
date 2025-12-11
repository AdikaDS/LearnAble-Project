package com.adika.learnable.util

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.adika.learnable.databinding.DialogDeleteConfirmBinding

object DeleteConfirmDialog {

    fun show(
        fragment: Fragment,
        @StringRes titleRes: Int? = null,
        @StringRes messageRes: Int? = null,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val dialog = Dialog(fragment.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val binding =
            DialogDeleteConfirmBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        titleRes?.let { binding.tvTitle.setText(it) }
        messageRes?.let { binding.tvMessage.setText(it) }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnDelete.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        dialog.setOnDismissListener { onCancel() }

        dialog.show()
    }
}