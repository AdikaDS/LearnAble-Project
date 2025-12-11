package com.adika.learnable.view.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogLanguageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageDialog : BottomSheetDialogFragment() {
    private var _binding: DialogLanguageBinding? = null
    private val binding get() = _binding!!

    private var onLanguageApplied: ((ChooseLanguageOptions) -> Unit)? = null
    private var currentOptions: ChooseLanguageOptions = ChooseLanguageOptions()

    companion object {
        fun newInstance(
            currentOptions: ChooseLanguageOptions? = null,
            onLanguageApplied: (ChooseLanguageOptions) -> Unit
        ): LanguageDialog {
            val fragment = LanguageDialog()
            fragment.onLanguageApplied = onLanguageApplied
            fragment.currentOptions = currentOptions ?: ChooseLanguageOptions()
            return fragment
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {

        binding.chipEnglish.isChecked = false
        binding.chipIndonesian.isChecked = false

        when (currentOptions.languageBy) {
            ChooseLanguageOptions.LanguageBy.INDONESIAN -> binding.chipIndonesian.isChecked = true
            ChooseLanguageOptions.LanguageBy.ENGLISH -> binding.chipEnglish.isChecked = true

        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.chipIndonesian.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                uncheckOtherLanguageChips(binding.chipIndonesian)
            } else {

                binding.chipIndonesian.isChecked = true
            }
        }

        binding.chipEnglish.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                uncheckOtherLanguageChips(binding.chipEnglish)
            } else {

                binding.chipEnglish.isChecked = true
            }
        }

        binding.btnSave.setOnClickListener { applyLanguage() }
    }

    private fun setupLanguageChipListener(chip: Chip) {
        when (chip) {
            binding.chipIndonesian -> chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    uncheckOtherLanguageChips(binding.chipIndonesian)
                } else {
                    binding.chipIndonesian.isChecked = true
                }
            }

            binding.chipEnglish -> chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    uncheckOtherLanguageChips(binding.chipEnglish)
                } else {
                    binding.chipEnglish.isChecked = true
                }
            }
        }
    }

    private fun uncheckOtherLanguageChips(selected: Chip) {

        val chips = listOf(binding.chipIndonesian, binding.chipEnglish)
        chips.forEach { chip ->
            chip.setOnCheckedChangeListener(null)
            if (chip != selected) {
                chip.isChecked = false
            }

            setupLanguageChipListener(chip)
        }
    }

    private fun applyLanguage() {
        val languageBy = when {
            binding.chipIndonesian.isChecked -> ChooseLanguageOptions.LanguageBy.INDONESIAN
            binding.chipEnglish.isChecked -> ChooseLanguageOptions.LanguageBy.ENGLISH
            else -> ChooseLanguageOptions.LanguageBy.INDONESIAN
        }
        onLanguageApplied?.invoke(ChooseLanguageOptions(languageBy))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ChooseLanguageOptions(
        val languageBy: LanguageBy = LanguageBy.INDONESIAN
    ) {
        enum class LanguageBy { INDONESIAN, ENGLISH }
    }
}