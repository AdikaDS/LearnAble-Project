package com.adika.learnable.view.auth

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.databinding.DialogWithOptionBinding

class LogoutDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "ResetPassSuccessDialog"
        const val REQ = "reset_success_req"
        const val ACTION = "action"
        const val ACTION_BACK_TO_LOGIN = "back_to_login"
    }

    private var _binding: DialogWithOptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogWithOptionBinding.inflate(layoutInflater)

        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            setCanceledOnTouchOutside(true)
            initUi()
        }
    }

    private fun initUi() {
        binding.btnYes.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQ, bundleOf(ACTION to ACTION_BACK_TO_LOGIN)
            )
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            dismiss()
        }
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