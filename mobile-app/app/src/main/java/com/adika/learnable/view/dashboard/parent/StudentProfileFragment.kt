package com.adika.learnable.view.dashboard.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.StudentProgressAdapter
import com.adika.learnable.databinding.FragmentStudentProfileBinding
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.User
import com.adika.learnable.viewmodel.dashboard.StudentProfileViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentProfileFragment : Fragment() {
    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!
    private val studentProfileViewModel: StudentProfileViewModel by viewModels()
    private lateinit var studentProgressAdapter: StudentProgressAdapter
    private val args: StudentProfileFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadStudentData()
    }

    private fun setupRecyclerView() {
        studentProgressAdapter = StudentProgressAdapter()
        binding.rvSubjectProgress.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = studentProgressAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        studentProfileViewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        studentProfileViewModel.overallProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        studentProfileViewModel.subjectProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is StudentProfileViewModel.StudentState.Loading,
            is StudentProfileViewModel.SubjectProgressState.Loading,
            is StudentProfileViewModel.OverallProgressState.Loading -> {
                showLoading(true)
            }

            is StudentProfileViewModel.StudentState.Success -> {
                showLoading(false)
                updateStudentUI(state.student)
            }

            is StudentProfileViewModel.SubjectProgressState.Success -> {
                showLoading(false)
                studentProgressAdapter.submitList(state.progress)
            }

            is StudentProfileViewModel.OverallProgressState.Success -> {
                state.progress?.let { progress ->
                    updateOverallProgressUI(progress)
                }
            }

            is StudentProfileViewModel.StudentState.Error,
            is StudentProfileViewModel.SubjectProgressState.Error,
            is StudentProfileViewModel.OverallProgressState.Error -> {
                showLoading(false)
                showToast(
                    (state as? StudentProfileViewModel.StudentState.Error)?.message
                        ?: (state as? StudentProfileViewModel.SubjectProgressState.Error)?.message
                        ?: (state as? StudentProfileViewModel.OverallProgressState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    private fun loadStudentData() {
        studentProfileViewModel.loadStudentData(args.studentId)
    }

    private fun updateStudentUI(student: User) {
        binding.apply {
            tvStudentName.text = student.name
            tvStudentEmail.text = student.email

            student.profilePicture.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(ivStudentProfile)
            }
        }
    }

    private fun updateOverallProgressUI(progress: StudentOverallProgress) {
        binding.apply {
            overallProgressBar.progress = progress.overallProgressPercentage
            tvOverallProgress.text = getString(R.string.progress_percentage, progress.overallProgressPercentage)
            tvCompletedSubjects.text = getString(R.string.completed_lessons, progress.completedSubjects, progress.totalSubjects)
            tvTotalTimeSpent.text = getString(R.string.total_time_spent, progress.totalTimeSpent)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 