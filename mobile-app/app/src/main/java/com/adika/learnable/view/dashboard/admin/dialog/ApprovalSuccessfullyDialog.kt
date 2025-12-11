package com.adika.learnable.view.dashboard.admin.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogBackToHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApprovalSuccessfullyDialog : DialogFragment() {

    companion object {
        const val TAG = "ApprovalSuccessDialog"
        const val REQ = "approval_success_req"
        const val ACTION = "action"
        const val ACTION_BACK_TO_DASHBOARD = "back_to_dashboard"
        const val ACTION_CLOSE = "close"
    }

    private var _binding: DialogBackToHomeBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogBackToHomeBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        dialog.setCanceledOnTouchOutside(true)
        setOnClickListener()
        initUi()

        return dialog
    }

    private fun initUi() {
        binding.tvTitle.text = getString(R.string.approval_success_title)
        binding.tvDesc.text = getString(R.string.approval_success_desc)
        binding.ivIcon.setImageResource(R.drawable.ic_success)
        binding.btnBackToHome.text = getString(R.string.back_to_homepage)
    }

    private fun setOnClickListener() {
        binding.btnBackToHome.setOnClickListener {

            parentFragmentManager.setFragmentResult(
                REQ, bundleOf(ACTION to ACTION_BACK_TO_DASHBOARD)
            )
            dismiss()
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        parentFragmentManager.setFragmentResult(
            REQ, bundleOf(ACTION to ACTION_CLOSE)
        )
        super.onCancel(dialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}