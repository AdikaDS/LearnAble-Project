package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.SubjectAdapter
import com.adika.learnable.databinding.FragmentStudentDashboardBinding
import com.adika.learnable.viewmodel.dashboard.StudentDashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentDashboardFragment : Fragment() {
    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentDashboardViewModel by viewModels()
    private lateinit var subjectAdapter: SubjectAdapter
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSchoolLevelDropdown()
        observeViewModel()
        setupClickListeners()
        
        if (isFirstLoad) {
            viewModel.loadUserData()
            isFirstLoad = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Only reload data if we have a selected school level
        viewModel.selectedSchoolLevel.value?.let {
            setupSchoolLevelDropdown()
            viewModel.reloadLastSelectedLevel()
        }
    }

    private fun setupRecyclerView() {
        subjectAdapter = SubjectAdapter { subject ->
            val state = viewModel.studentState.value

            if (state is StudentDashboardViewModel.StudentState.Success) {
                showLoading(false)
                val user = state.student

                if (user.disabilityType != null) {
                    val action = StudentDashboardFragmentDirections
                        .actionStudentDashboardToLessonList(
                            idSubject = subject.idSubject,
                            disabilityType = user.disabilityType
                        )
                    findNavController().navigate(action)
                } else {
                    showToast(getString(R.string.pick_disability))
                }
            } else {
                showToast(getString(R.string.fail_load_user_data))
            }
        }

        binding.subjectsRecyclerView.apply {
            adapter = subjectAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboard_to_editProfile)
        }
    }

    private fun setupSchoolLevelDropdown() {
        val schoolLevels = resources.getStringArray(R.array.school_levels)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, schoolLevels)
        binding.schoolLevelDropdown.apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                val selectedLevel = when (position) {
                    0 -> "sd"
                    1 -> "smp"
                    2 -> "sma"
                    else -> ""
                }
                viewModel.loadSubjectsBySchoolLevel(selectedLevel)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.subjectsState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.selectedSchoolLevel.observe(viewLifecycleOwner) { level ->
            // Update dropdown text if needed
            val schoolLevels = resources.getStringArray(R.array.school_levels)
            val position = when (level) {
                "sd" -> 0
                "smp" -> 1
                "sma" -> 2
                else -> -1
            }
            if (position >= 0) {
                binding.schoolLevelDropdown.setText(schoolLevels[position], false)
            }
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is StudentDashboardViewModel.StudentState.Loading -> {
                showLoading(true)
            }
            is StudentDashboardViewModel.SubjectState.Loading -> {
                showLoading(true)
            }

            is StudentDashboardViewModel.StudentState.Success -> {
                showLoading(false)
                state.student.let { student ->
                    binding.tvName.text = student.name
                    Log.d("StudentDashboard", "User loaded: $student")
                    Log.d("StudentDashboard", "Disability type: ${student.disabilityType}")
                }
            }

            is StudentDashboardViewModel.SubjectState.Success -> {
                showLoading(false)
                state.subject?.let { subject ->
                    subjectAdapter.submitList(subject)
                    binding.subjectsRecyclerView.visibility = if (subject.isEmpty()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    binding.emptyStateText.visibility = if (subject.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }

            is StudentDashboardViewModel.StudentState.Error,
            is StudentDashboardViewModel.SubjectState.Error -> {
                showLoading(false)
                showToast(
                    (state as? StudentDashboardViewModel.StudentState.Error)?.message
                        ?: (state as? StudentDashboardViewModel.SubjectState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
