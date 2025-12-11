package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.StudentSubjectAdapter
import com.adika.learnable.databinding.FragmentSubjectListBinding
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.util.GridSpacingItemDecoration
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.student.dialog.ChooseClassDialog
import com.adika.learnable.viewmodel.dashboard.StudentDashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentSubjectListFragment : BaseFragment() {
    private var _binding: FragmentSubjectListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentDashboardViewModel by viewModels()
    private lateinit var studentSubjectAdapter: StudentSubjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupSubjects()
        setupClickListeners()

        val schoolLevel = viewModel.selectedSchoolLevel.value ?: EducationLevels.SD
        viewModel.loadSubjectsBySchoolLevel(schoolLevel)

        setupTextScaling()
    }

    override fun onResume() {
        super.onResume()

        val currentState = viewModel.subjectsState.value
        if (currentState !is StudentDashboardViewModel.SubjectState.Success ||
            currentState.subject.isNullOrEmpty()
        ) {
            viewModel.selectedSchoolLevel.value?.let {
                viewModel.reloadLastSelectedLevel()
            }
        }
    }

    private fun setupSubjects() {
        val spanCount = resources.getInteger(R.integer.grid_span_subjects)
        val spacingPx = resources.getDimensionPixelSize(R.dimen.grid_spacing_12)

        val glm = GridLayoutManager(requireContext(), spanCount)
        binding.rvSubjects.layoutManager = glm
        binding.rvSubjects.setHasFixedSize(true)
        binding.rvSubjects.isNestedScrollingEnabled = false
        binding.rvSubjects.setPadding(0, 0, 0, 0)
        binding.rvSubjects.clipToPadding = false

        if (binding.rvSubjects.itemDecorationCount == 0) {
            binding.rvSubjects.addItemDecoration(
                GridSpacingItemDecoration(glm.spanCount, spacingPx, includeEdge = true)
            )
        }

        studentSubjectAdapter = StudentSubjectAdapter(
            onSubjectClick = { subject ->
                val action = StudentSubjectListFragmentDirections
                    .actionSubjectFragmentToLessonList(
                        idSubject = subject.idSubject,
                        subjectName = subject.name,
                        schoolLevel = subject.schoolLevel
                    )
                findNavController().navigate(action)
            }
        )
        binding.rvSubjects.adapter = studentSubjectAdapter

        studentSubjectAdapter.submitList(emptyList())
    }

    private fun setupClickListeners() {
        binding.apply {
            btnChooseClass.setOnClickListener {
                showClassFilterDialog()
            }
        }

        setupSearchFunctionality()
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchSubjects(s?.toString() ?: "")
            }
        })
    }

    private fun showClassFilterDialog() {
        val currentOptions = viewModel.sortFilterOptions.value
        val dialog = ChooseClassDialog.newInstance(
            currentOptions = currentOptions,
            onClassFilterApplied = { options ->
                viewModel.applySortFilter(options)
            }
        )
        dialog.show(parentFragmentManager, "ChooseClassDialog")
    }

    private fun observeViewModel() {
        viewModel.subjectsState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.selectedSchoolLevel.observe(viewLifecycleOwner) { level ->
            updateClassButtonText(level)
        }

        viewModel.sortFilterOptions.observe(viewLifecycleOwner) {
            updateClassButtonText(viewModel.selectedSchoolLevel.value)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is StudentDashboardViewModel.SubjectState.Loading -> {
                showLoading(true)
            }

            is StudentDashboardViewModel.SubjectState.Success -> {
                showLoading(false)
                state.subject?.let { subjects ->
                    studentSubjectAdapter.submitList(subjects) {

                        binding.rvSubjects.requestLayout()
                    }

                    if (subjects.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                    }
                } ?: run {
                    Log.d("StudentDashboard", "Subjects is null")
                    studentSubjectAdapter.submitList(emptyList())
                    showEmptyState(true)
                }
            }

            is StudentDashboardViewModel.SubjectState.Error -> {
                showLoading(false)
                val errorMessage = (state as? StudentDashboardViewModel.SubjectState.Error)?.message
                    ?: getString(R.string.unknown_error)

                showToast(errorMessage)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {

        binding.root.isEnabled = !isLoading

        if (isLoading) {
            binding.rvSubjects.visibility = View.GONE
        } else {
            binding.rvSubjects.visibility = View.VISIBLE
        }
    }

    private fun updateClassButtonText(level: String?) {
        val currentOptions = viewModel.sortFilterOptions.value
        val sd = getInitial(getString(R.string.elementarySchool))
        val smp = getInitial(getString(R.string.juniorHighSchool))
        val sma = getInitial(getString(R.string.seniorHighSchool))
        val buttonText = when (currentOptions?.filterBy) {
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSES -> sd
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSJHS -> smp
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSSHS -> sma
            null -> when (level) {
                EducationLevels.SD -> sd
                EducationLevels.SMP -> smp
                EducationLevels.SMA -> sma
                else -> getString(R.string.choose_level)
            }
        }
        binding.btnChooseClass.text = buttonText
    }

    private fun showEmptyState(isEmpty: Boolean) {

        if (isEmpty) {
            showToast("Tidak ada mata pelajaran ditemukan")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}