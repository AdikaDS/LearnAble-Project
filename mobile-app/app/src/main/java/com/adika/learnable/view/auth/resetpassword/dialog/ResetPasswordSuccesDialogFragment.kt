package com.adika.learnable.view.auth.resetpassword.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.databinding.DialogBackToHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordSuccessDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "ResetPassSuccessDialog"
        const val REQ = "reset_success_req"
        const val ACTION = "action"
        const val ACTION_BACK_TO_LOGIN = "back_to_login"
        const val ACTION_CLOSE = "close"
    }

    private var _binding: DialogBackToHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogBackToHomeBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        dialog.setCanceledOnTouchOutside(true)

        initUi()

        return dialog
    }

    private fun initUi() {
        binding.btnBackToHome.setOnClickListener {

            parentFragmentManager.setFragmentResult(
                REQ, bundleOf(ACTION to ACTION_BACK_TO_LOGIN)
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