package com.adika.learnable.view.dashboard.teacher.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogConfirmAnswerBinding

class ConfirmAnswerDialogFragment : DialogFragment() {
    private var _binding: DialogConfirmAnswerBinding? = null
    private val binding get() = _binding!!

    private var isCorrect: Boolean = true
    private var onConfirm: ((Boolean) -> Unit)? = null

    companion object {
        private const val ARG_IS_CORRECT = "is_correct"

        fun newInstance(
            isCorrect: Boolean,
            onConfirm: (Boolean) -> Unit
        ): ConfirmAnswerDialogFragment {
            return ConfirmAnswerDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_CORRECT, isCorrect)
                }
                this.onConfirm = onConfirm
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CenterDialog)
        arguments?.let {
            isCorrect = it.getBoolean(ARG_IS_CORRECT, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogConfirmAnswerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog?.setCanceledOnTouchOutside(true)
    }

    private fun setupViews() {
        binding.apply {

            if (isCorrect) {
                tvTitle.text = getString(R.string.jawaban_ini_benar)
                tvMessage.text = getString(R.string.confirm_correct_answer)
                ivIcon.setImageResource(R.drawable.icon_correct_answer)
            } else {
                tvTitle.text = getString(R.string.jawaban_ini_salah)
                tvMessage.text = getString(R.string.confirm_wrong_answer)
                ivIcon.setImageResource(R.drawable.icon_wrong_answer)
            }

            btnClose.setOnClickListener {
                dismiss()
            }

            btnConfirm.setOnClickListener {
                android.util.Log.d(
                    "ConfirmAnswerDialog",
                    "btnConfirm clicked, isCorrect=$isCorrect"
                )
                onConfirm?.invoke(isCorrect)
                dismiss()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}