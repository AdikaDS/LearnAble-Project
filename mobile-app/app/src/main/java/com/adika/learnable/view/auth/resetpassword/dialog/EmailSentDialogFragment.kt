package com.adika.learnable.view.auth.resetpassword.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogEmailSentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmailSentDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "EmailSentDialog"
        const val REQ = "email_sent_req"
        const val ACTION = "action"
        const val ARG_EMAIL = "email"

        fun newInstance(email: String) = EmailSentDialogFragment().apply {
            arguments = bundleOf(ARG_EMAIL to email)
        }
    }

    private var _binding: DialogEmailSentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEmailSentBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        dialog.setCanceledOnTouchOutside(true)

        initUi()

        return dialog
    }

    private fun initUi() {
        val email = arguments?.getString(ARG_EMAIL).orEmpty()
        val masked = maskEmail(email)
        binding.tvDesc.text = getString(R.string.email_sent_desc, masked)

        binding.btnResend.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQ, bundleOf(ACTION to "resend"))
            dismiss()
        }
        binding.btnChangeEmail.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQ, bundleOf(ACTION to "change"))
            dismiss()
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        parentFragmentManager.setFragmentResult(REQ, bundleOf(ACTION to "close"))
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

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val masked = if (name.length <= 2)
            "${name.first()}***"
        else
            "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
        return "$masked@${parts[1]}"
    }
}