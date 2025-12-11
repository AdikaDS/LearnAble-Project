package com.adika.learnable.view.settings

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.MaterialBookmarkAdapter
import com.adika.learnable.databinding.FragmentMaterialBookmarkBinding
import com.adika.learnable.model.Bookmark
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.util.CenteredImageSpan
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.student.progress.ProgressClassFilterDialog
import com.adika.learnable.viewmodel.lesson.MaterialBookmarkViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MaterialBookmarkFragment : BaseFragment() {
    private var _binding: FragmentMaterialBookmarkBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MaterialBookmarkViewModel by viewModels()
    private lateinit var bookmarkAdapter: MaterialBookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMaterialBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        view.post { setCustomText() }

        viewModel.loadBookmarks()

        setupTextScaling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        bookmarkAdapter = MaterialBookmarkAdapter(
            onBookmarkClick = { bookmark ->

                navigateToSubBabDetail(bookmark)
            },
            onRemoveBookmark = { bookmark ->

                bookmarkAdapter.updateBookmarkStatus(bookmark.id, false)

                viewModel.removeBookmark(bookmark.id)
            }
        )

        binding.rvBookmarks.apply {
            adapter = bookmarkAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnGradeFilter.setOnClickListener {
            showGradeFilterPopup()
        }

        binding.btnSubjectFilter.setOnClickListener {
            showSubjectFilterPopup()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                binding.loadingLayout.isVisible = uiState.isLoading
                binding.emptyLayout.isVisible = uiState.isEmpty && !uiState.isLoading
                binding.rvBookmarks.isVisible = !uiState.isEmpty && !uiState.isLoading

                uiState.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredBookmarks.collect { bookmarks ->
                bookmarkAdapter.submitList(bookmarks)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortFilterClassOptions.collect { options ->
                val sd = getInitial(getString(R.string.elementarySchool))
                val smp = getInitial(getString(R.string.juniorHighSchool))
                val sma = getInitial(getString(R.string.seniorHighSchool))
                val text = when (options?.filterBy) {
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES -> sd
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS -> smp
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS -> sma
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS -> getString(R.string.all_level)
                    null -> getString(R.string.all_level)
                }
                binding.btnGradeFilter.text = text

                updateSubjectButtonText()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSubjects.collect { selected ->
                val options = viewModel.sortFilterClassOptions.value

                val isLevelSelected = options != null
                val text = when {
                    !isLevelSelected -> getString(R.string.choose_level_first)
                    selected.isEmpty() -> getString(R.string.all_subject)
                    selected.size == 1 -> {
                        val subject =
                            viewModel.subjectsForLevel.value.find { it.idSubject in selected }
                        subject?.name ?: getString(R.string.all_subject)
                    }

                    else -> getString(R.string.all_subject)
                }
                binding.btnSubjectFilter.text = text
            }
        }
    }

    private fun updateSubjectButtonText() {
        val options = viewModel.sortFilterClassOptions.value
        val selected = viewModel.selectedSubjects.value
        val isLevelSelected = options != null

        val text = when {
            !isLevelSelected -> getString(R.string.choose_level_first)
            selected.isEmpty() -> getString(R.string.all_subject)
            selected.size == 1 -> {
                val subject = viewModel.subjectsForLevel.value.find { it.idSubject in selected }
                subject?.name ?: getString(R.string.all_subject)
            }

            else -> getString(R.string.all_subject)
        }
        binding.btnSubjectFilter.text = text
    }

    private fun navigateToSubBabDetail(bookmark: Bookmark) {

        viewLifecycleOwner.lifecycleScope.launch {

            val subBab = viewModel.getSubBabById(bookmark.subBabId)
            if (subBab == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.fail_load_subbab),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            try {
                val action =
                    MaterialBookmarkFragmentDirections.actionMaterialBookmarkFragmentToStudentSubBabDetailFragment(
                        subBab = subBab,
                        subjectName = bookmark.subjectName,
                        lessonName = bookmark.lessonTitle,
                        subjectId = bookmark.subjectId
                    )
                findNavController().navigate(action)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.unknown_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showGradeFilterPopup() {
        val dialog = ProgressClassFilterDialog.newInstance(
            currentSelection = viewModel.sortFilterClassOptions.value
                ?: ProgressClassFilterDialog.ClassFilterOptions(ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS),
            onClassFilterApplied = { classLevel ->
                val levelString = when (classLevel?.filterBy) {
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES -> "sd"
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS -> "smp"
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS -> "sma"
                    ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS -> null
                    null -> null
                }

                viewModel.filterByGrade(levelString)
            }
        )
        dialog.show(parentFragmentManager, "ProgressClassFilterDialog")
    }

    private fun setCustomText() {
        val d = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_bookmark)?.mutate()
            ?: return
        val lineH = binding.tvBookmarkHint.lineHeight.takeIf { it > 0 } ?: return
        val sizePx = (lineH * 0.9f).toInt().coerceAtLeast(1)
        d.setBounds(0, 0, sizePx, sizePx)
        d.setTint(ContextCompat.getColor(requireContext(), R.color.grey))

        val iconSpan = SpannableString("\uFFFc")
        iconSpan.setSpan(CenteredImageSpan(d), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvBookmarkHint.text =
            TextUtils.expandTemplate(getText(R.string.empty_bookmark_desc), iconSpan)
    }

    private fun showSubjectFilterPopup() {
        val subjectOptions =
            listOf(getString(R.string.all_subject)) + viewModel.subjectsForLevel.value.map { it.name }
        if (subjectOptions.isEmpty()) return

        val currentIds = viewModel.selectedSubjects.value
        val currentIndex = if (currentIds.isEmpty()) 0 else {
            val first = viewModel.subjectsForLevel.value.find { it.idSubject in currentIds }
            if (first != null) subjectOptions.indexOf(first.name) else 0
        }

        val popup = ListPopupWindow(requireContext())
        val adapter = object : ArrayAdapter<String>(requireContext(), 0, subjectOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_dropdown, parent, false)
                val textView = view.findViewById<TextView>(R.id.tvText)
                val activated = position == currentIndex
                textView.isActivated = activated
                textView.text = getItem(position)
                textView.setTextColor(
                    if (activated) android.graphics.Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }
        popup.setAdapter(adapter)
        popup.anchorView = binding.btnSubjectFilter
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_dropdown_panel
            )
        )

        binding.btnSubjectFilter.post {
            val paint = binding.btnSubjectFilter.paint
            var maxWidth = 0
            for (option in subjectOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) maxWidth = textWidth
            }
            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding
            if (popup.width < binding.btnSubjectFilter.width) popup.width =
                binding.btnSubjectFilter.width
            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                viewModel.setSelectedSubjects(emptySet())
            } else {
                val selected = viewModel.subjectsForLevel.value[position - 1]
                viewModel.setSelectedSubjects(setOf(selected.idSubject))
            }
            popup.dismiss()
        }
    }
}