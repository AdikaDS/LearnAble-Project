package com.adika.learnable.view.dashboard.student.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogChooseClassBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseClassDialog : BottomSheetDialogFragment() {

    private var _binding: DialogChooseClassBinding? = null
    private val binding get() = _binding!!

    private var onClassFilterApplied: ((ClassFilterOptions) -> Unit)? = null
    private var currentOptions: ClassFilterOptions = ClassFilterOptions()

    private lateinit var chips: List<Chip>
    private var isUpdating = false

    companion object {
        private const val STATE_SELECTED_INDEX = "selected_index"
        fun newInstance(
            currentOptions: ClassFilterOptions? = null,
            onClassFilterApplied: (ClassFilterOptions) -> Unit
        ): ChooseClassDialog {
            val fragment = ChooseClassDialog()
            fragment.onClassFilterApplied = onClassFilterApplied
            fragment.currentOptions = currentOptions ?: ClassFilterOptions()
            return fragment
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChooseClassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        savedInstanceState?.getInt(STATE_SELECTED_INDEX)?.let { selectIndex(it) }
        setupClickListeners()
    }

    private fun initChips() {
        chips = listOf(
            binding.chipClassES, binding.chipClassJHS, binding.chipClassSHS
        )
        chips.forEachIndexed { index, chip -> chip.tag = index }
    }

    private fun setupUI() {
        initChips()

        isUpdating = true
        chips.forEach { it.isChecked = false }
        isUpdating = false

        val idx = currentOptions.filterBy.ordinal.coerceIn(chips.indices)
        selectIndex(idx)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        val singleChoiceListener =
            CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (isUpdating) return@OnCheckedChangeListener

                val clickedIndex = (buttonView.tag as? Int) ?: return@OnCheckedChangeListener
                if (!isChecked) {

                    val anyOtherChecked = chips.any { it != buttonView && it.isChecked }
                    if (!anyOtherChecked) {
                        isUpdating = true
                        (buttonView as Chip).isChecked = true
                        isUpdating = false
                    }
                    return@OnCheckedChangeListener
                }

                selectIndex(clickedIndex)
            }

        chips.forEach { it.setOnCheckedChangeListener(singleChoiceListener) }

        binding.btnSave.setOnClickListener { applySortFilter() }
    }

    private fun selectIndex(index: Int) {
        isUpdating = true
        chips.forEachIndexed { i, chip -> chip.isChecked = (i == index) }
        isUpdating = false
    }

    private fun getSelectedIndex(): Int {
        val idx = chips.indexOfFirst { it.isChecked }
        return if (idx >= 0) idx else 0
    }

    private fun applySortFilter() {
        val selectedIndex = getSelectedIndex()
        val selectedFilter = ClassFilterOptions.FilterBy.entries[selectedIndex]
        onClassFilterApplied?.invoke(ClassFilterOptions(selectedFilter))
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_INDEX, getSelectedIndex())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ClassFilterOptions(
        val filterBy: FilterBy = FilterBy.CLASSES
    ) {
        enum class FilterBy {
            CLASSES, CLASSJHS, CLASSSHS
        }
    }
}