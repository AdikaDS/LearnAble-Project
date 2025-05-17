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
import com.adika.learnable.adapter.ProgressAdapter
import com.adika.learnable.databinding.FragmentStudentProfileBinding
import com.adika.learnable.model.User
import com.adika.learnable.viewmodel.dashboard.ParentDashboardViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentProfileFragment : Fragment() {
    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var progressAdapter: ProgressAdapter
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
        loadStudents()
    }

    private fun setupRecyclerView() {
        progressAdapter = ProgressAdapter()
        binding.rvProgress.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = progressAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ParentDashboardViewModel.StudentState.Loading -> {
                    showLoading(true)
                }
                is ParentDashboardViewModel.StudentState.Success -> {
                    showLoading(false)
                    // Cari siswa yang sesuai dengan studentId
                    val student = state.students.find { it.id == args.studentId }
                    student?.let {
                        updateStudentUI(it)
                        viewModel.loadStudentProgress(it.id)
                    }

                }
                is ParentDashboardViewModel.StudentState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.studentProgressState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ParentDashboardViewModel.StudentProgressState.Loading -> {
                    showLoading(true)
                }
                is ParentDashboardViewModel.StudentProgressState.Success -> {
                    showLoading(false)
                    progressAdapter.submitList(state.progressList)
                }
                is ParentDashboardViewModel.StudentProgressState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun loadStudents() {
        viewModel.loadStudents()
    }

    private fun updateStudentUI(student: User) {
        binding.apply {
            tvStudentName.text = student.name
            tvStudentEmail.text = student.email
            tvDisabilityType.text = student.disabilityType

            // Load profile picture
            student.profilePicture.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .circleCrop()
                    .into(ivStudentProfile)
            }
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