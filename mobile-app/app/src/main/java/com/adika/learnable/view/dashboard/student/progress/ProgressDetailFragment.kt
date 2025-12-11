package com.adika.learnable.view.dashboard.student.progress

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.LessonProgressAdapter
import com.adika.learnable.adapter.OverallProgressAdapter
import com.adika.learnable.adapter.SubBabDoneAdapter
import com.adika.learnable.adapter.SubBabProgressAdapter
import com.adika.learnable.adapter.SubjectProgressAdapter
import com.adika.learnable.adapter.WeeklyHistoryAdapter
import com.adika.learnable.databinding.FragmentProgressDetailBinding
import com.adika.learnable.model.LessonProgressItem
import com.adika.learnable.model.SubBabDoneItem
import com.adika.learnable.model.SubBabProgressItem
import com.adika.learnable.model.SubjectProgressItem
import com.adika.learnable.model.WeekGroup
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.viewmodel.progress.ProgressDetailViewModel
import com.adika.learnable.viewmodel.progress.ProgressViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible

@AndroidEntryPoint
class ProgressDetailFragment : Fragment() {
    private var _binding: FragmentProgressDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProgressViewModel by viewModels()
    private val progressDetailViewModel: ProgressDetailViewModel by viewModels()
    private lateinit var historyAdapter: WeeklyHistoryAdapter
    private lateinit var overallProgressAdapter: OverallProgressAdapter
    private lateinit var subjectProgressAdapter: SubjectProgressAdapter
    private lateinit var lessonProgressAdapter: LessonProgressAdapter
    private lateinit var subBabProgressAdapter: SubBabProgressAdapter
    private lateinit var subBabDoneAdapter: SubBabDoneAdapter

    private var lastSubjectProgress: List<SubjectProgressItem> = emptyList()
    private var lastLessonProgress: List<LessonProgressItem> = emptyList()
    private var lastSubBabProgress: List<SubBabProgressItem> = emptyList()
    private var lastSubBabDone: List<SubBabDoneItem> = emptyList()
    private var lastWeekGroups: List<WeekGroup> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        val currentFilter = viewModel.learnFilter.value ?: ProgressViewModel.LearnFilter.HISTORY
        updateLayoutVisibility(currentFilter)

        viewModel.loadRecentHistory()
        progressDetailViewModel.loadAllProgress()
    }

    private fun setupRecyclerView() {

        historyAdapter = WeeklyHistoryAdapter()
        binding.rvHistoryLearn.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistoryLearn.adapter = historyAdapter

        overallProgressAdapter = OverallProgressAdapter()
        binding.rvOverallProgress.adapter = overallProgressAdapter
        binding.rvOverallProgress.layoutManager = LinearLayoutManager(requireContext())

        subjectProgressAdapter = SubjectProgressAdapter()
        binding.rvSubjectProgress.adapter = subjectProgressAdapter
        binding.rvSubjectProgress.layoutManager = LinearLayoutManager(requireContext())

        lessonProgressAdapter = LessonProgressAdapter()
        binding.rvLessonProgress.adapter = lessonProgressAdapter
        binding.rvLessonProgress.layoutManager = LinearLayoutManager(requireContext())

        subBabProgressAdapter = SubBabProgressAdapter()
        binding.rvSubbabProgress.adapter = subBabProgressAdapter
        binding.rvSubbabProgress.layoutManager = LinearLayoutManager(requireContext())

        subBabDoneAdapter = SubBabDoneAdapter()
        binding.rvDoneProgress.adapter = subBabDoneAdapter
        binding.rvDoneProgress.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.btnDropDownLearn.setOnClickListener {
            showLearnFilterDropdown()
        }

        binding.btnLevel.setOnClickListener {
            showClassFilterDialog()
        }

        binding.btnSubject.setOnClickListener {
            showSubjectFilterDialog()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {

        viewModel.history.observe(viewLifecycleOwner) { history ->
            lastWeekGroups = history
            applyProgressFilters()
        }

        viewModel.learnFilter.observe(viewLifecycleOwner) { filter ->
            updateLearnButtonText(filter)
            updateLayoutVisibility(filter)
        }

        viewModel.sortFilterClassOptions.observe(viewLifecycleOwner) { options ->
            updateLevelButtonText(options)
            applyProgressFilters()
        }

        viewModel.selectedSubjects.observe(viewLifecycleOwner) { subjects ->
            updateSubjectButtonText(subjects)
            applyProgressFilters()
        }

        progressDetailViewModel.overallProgress.observe(viewLifecycleOwner) { overallProgress ->
            overallProgressAdapter.submitList(overallProgress)
            val isEmpty = overallProgress.isEmpty()
            binding.rvOverallProgress.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        progressDetailViewModel.subjectProgress.observe(viewLifecycleOwner) { subjectProgress ->
            lastSubjectProgress = subjectProgress
            applyProgressFilters()
        }

        progressDetailViewModel.lessonProgress.observe(viewLifecycleOwner) { lessonProgress ->
            lastLessonProgress = lessonProgress
            applyProgressFilters()
        }

        progressDetailViewModel.subBabProgress.observe(viewLifecycleOwner) { subBabProgress ->
            lastSubBabProgress = subBabProgress
            applyProgressFilters()
        }

        progressDetailViewModel.subBabDoneProgress.observe(viewLifecycleOwner) { subBabDoneProgress ->
            lastSubBabDone = subBabDoneProgress
            applyProgressFilters()
        }

        progressDetailViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProgressDetailViewModel.UiState.Loading -> {
                    showDetailLoading(true)
                }

                is ProgressDetailViewModel.UiState.Success -> {
                    showDetailLoading(false)
                }

                is ProgressDetailViewModel.UiState.Error -> {
                    showDetailLoading(false)

                }
            }
        }
    }

    private fun showClassFilterDialog() {
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
                viewModel.applyClassFilter(levelString)
            }
        )
        dialog.show(parentFragmentManager, "ProgressClassFilterDialog")
    }

    private fun showLearnFilterDropdown() {
        val learnOptions = viewModel.getProgressFilterOptions()
        val currentFilter = viewModel.learnFilter.value ?: ProgressViewModel.LearnFilter.HISTORY
        val currentIndex = when (currentFilter) {
            ProgressViewModel.LearnFilter.HISTORY -> 0
            ProgressViewModel.LearnFilter.PROGRESS -> 1
            ProgressViewModel.LearnFilter.COMPLETED -> 2
        }

        val popup = ListPopupWindow(requireContext())

        val adapter = object : ArrayAdapter<String>(requireContext(), 0, learnOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_dropdown, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvText)

                val activated = position == currentIndex
                textView.isActivated = activated
                textView.text = getItem(position)

                textView.setTextColor(
                    if (activated) Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }

        popup.setAdapter(adapter)
        popup.anchorView = binding.btnDropDownLearn
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_dropdown_panel
            )
        )

        binding.btnDropDownLearn.post {
            val paint = binding.btnDropDownLearn.paint
            var maxWidth = 0
            for (option in learnOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }

            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding

            if (popup.width < binding.btnDropDownLearn.width) {
                popup.width = binding.btnDropDownLearn.width
            }

            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = when (position) {
                0 -> ProgressViewModel.LearnFilter.HISTORY
                1 -> ProgressViewModel.LearnFilter.PROGRESS
                2 -> ProgressViewModel.LearnFilter.COMPLETED
                else -> ProgressViewModel.LearnFilter.HISTORY
            }
            viewModel.setLearnFilter(selectedFilter)
            popup.dismiss()
        }
    }

    private fun showSubjectFilterDialog() {
        val subjects = viewModel.subjectsForLevel.value ?: emptyList()
        val preselected = viewModel.selectedSubjects.value ?: emptySet()

        val subjectOptions = mutableListOf(getString(R.string.all_subject))
        subjectOptions.addAll(subjects.map { it.name })

        val currentIndex = if (preselected.isEmpty()) 0 else {

            val firstSelectedSubject = subjects.find { it.idSubject in preselected }
            if (firstSelectedSubject != null) {
                subjectOptions.indexOf(firstSelectedSubject.name)
            } else 0
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
                    if (activated) Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }

        popup.setAdapter(adapter)
        popup.anchorView = binding.btnSubject
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_dropdown_panel
            )
        )

        binding.btnSubject.post {
            val paint = binding.btnSubject.paint
            var maxWidth = 0
            for (option in subjectOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }

            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding

            if (popup.width < binding.btnSubject.width) {
                popup.width = binding.btnSubject.width
            }

            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val selectedSubjects = when (position) {
                0 -> emptySet() //
                else -> {
                    val selectedSubject =
                        subjects[position - 1] // -1 karena index 0 adalah "Semua Pelajaran"
                    setOf(selectedSubject.idSubject)
                }
            }
            viewModel.setSelectedSubjects(selectedSubjects)
            popup.dismiss()
        }
    }

    private fun updateLearnButtonText(filter: ProgressViewModel.LearnFilter) {
        val text = when (filter) {
            ProgressViewModel.LearnFilter.HISTORY -> getString(R.string.learning_history)
            ProgressViewModel.LearnFilter.PROGRESS -> getString(R.string.title_progress_screen)
            ProgressViewModel.LearnFilter.COMPLETED -> getString(R.string.done)
        }
        binding.btnDropDownLearn.text = text
    }

    private fun updateLevelButtonText(options: ProgressClassFilterDialog.ClassFilterOptions?) {
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
        binding.btnLevel.text = text
    }

    private fun updateSubjectButtonText(selectedSubjects: Set<String>) {
        val isLevelSelected = viewModel.sortFilterClassOptions.value != null
        val text = when {
            !isLevelSelected -> getString(R.string.choose_level_first)
            selectedSubjects.isEmpty() -> getString(R.string.all_subject)
            selectedSubjects.size == 1 -> {

                val subject =
                    viewModel.subjectsForLevel.value?.find { it.idSubject in selectedSubjects }
                subject?.name ?: getString(R.string.all_subject)
            }

            else -> getString(R.string.all_subject)
        }
        binding.btnSubject.text = text
    }

    private fun updateLayoutVisibility(filter: ProgressViewModel.LearnFilter) {
        if (binding.progressBarDetail.isVisible) {
            binding.layoutHistoryLearn.visibility = View.GONE
            binding.layoutProgress.visibility = View.GONE
            binding.layoutDoneLearn.visibility = View.GONE
            return
        }

        binding.layoutHistoryLearn.visibility = View.GONE
        binding.layoutProgress.visibility = View.GONE
        binding.layoutDoneLearn.visibility = View.GONE

        when (filter) {
            ProgressViewModel.LearnFilter.HISTORY -> {
                binding.layoutHistoryLearn.visibility = View.VISIBLE
            }

            ProgressViewModel.LearnFilter.PROGRESS -> {
                binding.layoutProgress.visibility = View.VISIBLE
            }

            ProgressViewModel.LearnFilter.COMPLETED -> {
                binding.layoutDoneLearn.visibility = View.VISIBLE
            }
        }
    }

    private fun applyProgressFilters() {
        val level = when (viewModel.sortFilterClassOptions.value?.filterBy) {
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES -> "sd"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS -> "smp"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS -> "sma"
            else -> null
        }
        val selectedSubjectIds = viewModel.selectedSubjects.value ?: emptySet()

        val filteredWeekGroups = lastWeekGroups.map { group ->
            val items = group.items.filter { item ->
                val levelOk = level == null || item.schoolLevel == level
                val subjectOk =
                    selectedSubjectIds.isEmpty() || (item.subjectId != null && selectedSubjectIds.contains(
                        item.subjectId
                    ))
                levelOk && subjectOk
            }
            WeekGroup(group.title, items)
        }.filter { it.items.isNotEmpty() }
        historyAdapter.submitList(filteredWeekGroups)
        val historyEmpty = filteredWeekGroups.isEmpty()

        val filteredSubjects = lastSubjectProgress.filter { item ->
            val levelOk = level == null || item.subject.schoolLevel == level
            val subjectOk =
                selectedSubjectIds.isEmpty() || selectedSubjectIds.contains(item.subject.idSubject)
            levelOk && subjectOk
        }
        subjectProgressAdapter.submitList(filteredSubjects)
        val subjectsEmpty = filteredSubjects.isEmpty()
        binding.tvTitleSubjectProgress.visibility = if (subjectsEmpty) View.GONE else View.VISIBLE
        binding.rvSubjectProgress.visibility = if (subjectsEmpty) View.GONE else View.VISIBLE

        val filteredLessons = lastLessonProgress.filter { item ->
            val levelOk = level == null || item.lesson.schoolLevel == level
            val subjectOk =
                selectedSubjectIds.isEmpty() || selectedSubjectIds.contains(item.lesson.idSubject)
            levelOk && subjectOk
        }
        lessonProgressAdapter.submitList(filteredLessons)
        val lessonsEmpty = filteredLessons.isEmpty()
        binding.tvTitleLessonProgress.visibility = if (lessonsEmpty) View.GONE else View.VISIBLE
        binding.rvLessonProgress.visibility = if (lessonsEmpty) View.GONE else View.VISIBLE

        val filteredSubBabProgress = lastSubBabProgress.filter { item ->
            val lessonLevel = (item.progress.lessonId).let { lessonId ->
                filteredLessons.find { it.progress.lessonId == lessonId }?.lesson?.schoolLevel
            }
            val lessonSubject = (item.progress.lessonId).let { lessonId ->
                filteredLessons.find { it.progress.lessonId == lessonId }?.lesson?.idSubject
            }
            val levelOk = level == null || lessonLevel == level
            val subjectOk =
                selectedSubjectIds.isEmpty() || (lessonSubject != null && selectedSubjectIds.contains(
                    lessonSubject
                ))
            levelOk && subjectOk
        }
        subBabProgressAdapter.submitList(filteredSubBabProgress)
        val subbabsEmpty = filteredSubBabProgress.isEmpty()
        binding.tvTitleSubbabProgress.visibility = if (subbabsEmpty) View.GONE else View.VISIBLE
        binding.rvSubbabProgress.visibility = if (subbabsEmpty) View.GONE else View.VISIBLE

        val filteredSubBabDone = lastSubBabDone.filter { item ->
            val lessonLevel = (item.progress.lessonId).let { lessonId ->
                filteredLessons.find { it.progress.lessonId == lessonId }?.lesson?.schoolLevel
            }
            val lessonSubject = (item.progress.lessonId).let { lessonId ->
                filteredLessons.find { it.progress.lessonId == lessonId }?.lesson?.idSubject
            }
            val levelOk = level == null || lessonLevel == level
            val subjectOk =
                selectedSubjectIds.isEmpty() || (lessonSubject != null && selectedSubjectIds.contains(
                    lessonSubject
                ))
            levelOk && subjectOk
        }
        subBabDoneAdapter.submitList(filteredSubBabDone)
        val subbabsDoneEmpty = filteredSubBabDone.isEmpty()

        val showCombinedEmpty = subjectsEmpty && lessonsEmpty && subbabsEmpty
        binding.emptyProgress.root.visibility = if (showCombinedEmpty) View.VISIBLE else View.GONE
        binding.emptyHistoryProgress.root.visibility = if (historyEmpty) View.VISIBLE else View.GONE
        binding.emptyDoneProgress.root.visibility =
            if (subbabsDoneEmpty) View.VISIBLE else View.GONE

    }

    private fun showDetailLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarDetail.visibility = View.VISIBLE
            binding.layoutHistoryLearn.visibility = View.GONE
            binding.layoutProgress.visibility = View.GONE
            binding.layoutDoneLearn.visibility = View.GONE
        } else {
            binding.progressBarDetail.visibility = View.GONE
            val currentFilter = viewModel.learnFilter.value ?: ProgressViewModel.LearnFilter.HISTORY
            updateLayoutVisibility(currentFilter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}