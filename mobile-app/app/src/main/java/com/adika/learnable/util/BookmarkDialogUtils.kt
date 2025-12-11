package com.adika.learnable.util

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import com.adika.learnable.databinding.DialogBookmarkSuccessBinding
import com.adika.learnable.databinding.DialogDeleteConfirmBinding

object BookmarkDialogUtils {

    fun showBookmarkSuccessDialog(
        fragment: Fragment,
        onDismiss: () -> Unit = {}
    ) {
        val dialog = Dialog(fragment.requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val binding =
            DialogBookmarkSuccessBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnOk.setOnClickListener {
            dialog.dismiss()
            onDismiss()
        }

        dialog.setOnDismissListener {
            onDismiss()
        }

        dialog.show()
    }

    fun showBookmarkDeleteConfirmDialog(
        fragment: Fragment,
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

        binding.btnCancel.setOnClickListener {
            Log.d("BookmarkDialogUtils", "Delete button clicked")
            dialog.dismiss()
            onConfirm() // btnCancel is actually the delete button
        }

        binding.btnDelete.setOnClickListener {
            Log.d("BookmarkDialogUtils", "Cancel button clicked")
            dialog.dismiss()
            onCancel() // btnDelete is actually the cancel button
        }

        dialog.setOnDismissListener {
            onCancel()
        }

        dialog.show()
    }
}