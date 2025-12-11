package com.adika.learnable.view.dashboard.admin.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogWithOptionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmationApprovalDialog : DialogFragment() {
    companion object {
        const val TAG = "ConfirmationApprovalDialog"
        const val REQ = "confirmation_approval_req"
        const val ACTION = "action"
        const val ACTION_APPROVE = "approve"
        const val ACTION_BACK_TO_DASHBOARD = "back_to_dashboard"
    }

    private var _binding: DialogWithOptionBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            ApprovalSuccessfullyDialog.REQ, this
        ) { _, bundle ->
            when (bundle.getString(ApprovalSuccessfullyDialog.ACTION)) {
                ApprovalSuccessfullyDialog.ACTION_BACK_TO_DASHBOARD,
                ApprovalSuccessfullyDialog.ACTION_CLOSE -> {
                    parentFragmentManager.setFragmentResult(
                        REQ, bundleOf(ACTION to ACTION_BACK_TO_DASHBOARD)
                    )
                    dismiss()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogWithOptionBinding.inflate(layoutInflater)

        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            setCanceledOnTouchOutside(true)
            setOnClickListener()
            initUi()
        }
    }

    private fun initUi() {
        binding.tvTitle.text = getString(R.string.confirmation_approval_title)
        binding.tvDesc.text = getString(R.string.confirmation_approval_desc)
        binding.btnYes.text = getString(R.string.approve)
        binding.btnNo.text = getString(R.string.no)
        binding.btnYes.setBackgroundResource(R.drawable.bg_button_blue)
        binding.btnNo.setBackgroundResource(R.drawable.bg_button_grey)
        binding.ivIcon.setImageResource(R.drawable.ic_warning)
        binding.bgIv.setBackgroundResource(R.drawable.bg_circle_soft_yellow)
    }

    private fun setOnClickListener() {
        binding.btnYes.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQ, bundleOf(ACTION to ACTION_APPROVE)
            )
            showSuccessApproval()
        }

        binding.btnNo.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQ, bundleOf(ACTION to ACTION_BACK_TO_DASHBOARD)
            )
            dismiss()
        }
    }

    private fun showSuccessApproval() {
        ApprovalSuccessfullyDialog().show(childFragmentManager, ApprovalSuccessfullyDialog.TAG)
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