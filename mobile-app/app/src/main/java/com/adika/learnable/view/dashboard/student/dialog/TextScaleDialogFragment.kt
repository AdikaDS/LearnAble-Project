package com.adika.learnable.view.dashboard.student.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogTextScaleBinding
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.setupTextScaling
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TextScaleDialogFragment : DialogFragment() {

    private var _binding: DialogTextScaleBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var textScaleRepository: TextScaleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTextScaleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme(): Int = R.style.CenterDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scale = textScaleRepository.getScale()
        val checkedId = when {
            scale <= 0.95f -> R.id.btnSmall
            scale >= 1.1f -> R.id.btnLarge
            else -> R.id.btnMedium
        }
        setCheckedChip(checkedId)

        val chips = listOf(binding.btnSmall, binding.btnMedium, binding.btnLarge)
        chips.forEach { chip ->
            chip.setOnClickListener { setCheckedChip(chip.id) }
        }

        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            val selectedScale = when {
                binding.btnSmall.isChecked -> 0.9f
                binding.btnLarge.isChecked -> 1.15f
                else -> 1.0f
            }
            android.util.Log.d("TextScaleDialog", "Saving scale: $selectedScale")
            textScaleRepository.setScale(selectedScale)

            parentFragmentManager.setFragmentResult(
                "text_scale_changed",
                bundleOf("scale" to selectedScale)
            )
            android.util.Log.d(
                "TextScaleDialog",
                "Sent text_scale_changed result via parentFragmentManager"
            )

            dismiss()
        }

        setupTextScaling()
    }

    private fun setCheckedChip(idToCheck: Int) {
        binding.btnSmall.isChecked = idToCheck == R.id.btnSmall
        binding.btnMedium.isChecked = idToCheck == R.id.btnMedium
        binding.btnLarge.isChecked = idToCheck == R.id.btnLarge
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}