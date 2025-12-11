package com.adika.learnable.view.dashboard.admin.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogSortFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SortFilterDialog : BottomSheetDialogFragment() {

    private var _binding: DialogSortFilterBinding? = null
    private val binding get() = _binding!!

    private var onSortFilterApplied: ((SortFilterOptions) -> Unit)? = null
    private var currentOptions: SortFilterOptions = SortFilterOptions()

    private lateinit var filterChips: List<Chip>
    private lateinit var sortChips: List<Chip>

    private var isUpdating = false

    companion object {
        private const val KEY_SELECTED_FILTER_INDEX = "selected_filter_index"
        private const val KEY_SELECTED_SORT_INDEX = "selected_sort_index"

        fun newInstance(
            currentOptions: SortFilterOptions? = null,
            onSortFilterApplied: (SortFilterOptions) -> Unit
        ): SortFilterDialog {
            return SortFilterDialog().apply {
                this.onSortFilterApplied = onSortFilterApplied
                this.currentOptions = currentOptions ?: SortFilterOptions()
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSortFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterChips =
            listOf(binding.chipDate, binding.chipName, binding.chipRole, binding.chipStatus)
        sortChips = listOf(binding.chipAscending, binding.chipDescending)

        filterChips.forEachIndexed { i, c -> c.tag = i }
        sortChips.forEachIndexed { i, c -> c.tag = i }

        setupUI()

        savedInstanceState?.let {
            val fi = it.getInt(KEY_SELECTED_FILTER_INDEX, getSelectedIndex(filterChips))
            val si = it.getInt(KEY_SELECTED_SORT_INDEX, getSelectedIndex(sortChips))
            selectIndex(filterChips, fi)
            selectIndex(sortChips, si)
        }

        setupClickListeners()
    }

    private fun setupUI() {

        isUpdating = true
        filterChips.forEach { it.isChecked = false }
        sortChips.forEach { it.isChecked = false }
        isUpdating = false

        val filterIndex = when (currentOptions.filterBy) {
            SortFilterOptions.FilterBy.DATE -> 0
            SortFilterOptions.FilterBy.NAME -> 1
            SortFilterOptions.FilterBy.ROLE -> 2
            SortFilterOptions.FilterBy.STATUS -> 3
        }
        val sortIndex = when (currentOptions.sortOrder) {
            SortFilterOptions.SortOrder.ASCENDING -> 0
            SortFilterOptions.SortOrder.DESCENDING -> 1
        }

        selectIndex(filterChips, filterIndex)
        selectIndex(sortChips, sortIndex)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        val filterListener = makeSingleChoiceListener(filterChips, selectionRequired = true)
        val sortListener = makeSingleChoiceListener(sortChips, selectionRequired = true)

        filterChips.forEach { it.setOnCheckedChangeListener(filterListener) }
        sortChips.forEach { it.setOnCheckedChangeListener(sortListener) }

        binding.btnSave.setOnClickListener { applySortFilter() }
    }

    /**
     * Listener generik single-choice untuk sebuah grup chip.
     * Menjamin exactly-one-checked jika selectionRequired = true.
     */
    private fun makeSingleChoiceListener(
        group: List<Chip>,
        selectionRequired: Boolean
    ): CompoundButton.OnCheckedChangeListener {
        return CompoundButton.OnCheckedChangeListener { button, isChecked ->
            if (isUpdating) return@OnCheckedChangeListener

            val clickedIndex = (button.tag as? Int) ?: return@OnCheckedChangeListener

            if (!isChecked) {
                if (selectionRequired) {

                    val anyOtherChecked = group.any { it != button && it.isChecked }
                    if (!anyOtherChecked) {
                        isUpdating = true
                        (button as Chip).isChecked = true
                        isUpdating = false
                    }
                }
                return@OnCheckedChangeListener
            }

            selectIndex(group, clickedIndex)
        }
    }

    private fun selectIndex(group: List<Chip>, index: Int) {
        val safeIndex = index.coerceIn(group.indices)
        isUpdating = true
        group.forEachIndexed { i, chip -> chip.isChecked = (i == safeIndex) }
        isUpdating = false
    }

    private fun getSelectedIndex(group: List<Chip>): Int {
        val idx = group.indexOfFirst { it.isChecked }
        return if (idx >= 0) idx else 0
    }

    private fun applySortFilter() {
        val filterIndex = getSelectedIndex(filterChips)
        val sortIndex = getSelectedIndex(sortChips)

        val filterBy = when (filterIndex) {
            0 -> SortFilterOptions.FilterBy.DATE
            1 -> SortFilterOptions.FilterBy.NAME
            2 -> SortFilterOptions.FilterBy.ROLE
            3 -> SortFilterOptions.FilterBy.STATUS
            else -> SortFilterOptions.FilterBy.DATE
        }
        val sortOrder = when (sortIndex) {
            0 -> SortFilterOptions.SortOrder.ASCENDING
            1 -> SortFilterOptions.SortOrder.DESCENDING
            else -> SortFilterOptions.SortOrder.ASCENDING
        }

        onSortFilterApplied?.invoke(SortFilterOptions(filterBy, sortOrder))
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_FILTER_INDEX, getSelectedIndex(filterChips))
        outState.putInt(KEY_SELECTED_SORT_INDEX, getSelectedIndex(sortChips))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class SortFilterOptions(
        val filterBy: FilterBy = FilterBy.DATE,
        val sortOrder: SortOrder = SortOrder.ASCENDING
    ) {
        enum class FilterBy { DATE, NAME, ROLE, STATUS }
        enum class SortOrder { ASCENDING, DESCENDING }
    }
}