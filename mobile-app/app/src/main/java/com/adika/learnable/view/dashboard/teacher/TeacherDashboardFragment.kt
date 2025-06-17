package com.adika.learnable.view.dashboard.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.StudentAdapter
import com.adika.learnable.adapter.StudentProgressAdapter
import com.adika.learnable.databinding.FragmentTeacherDashboardBinding
import com.adika.learnable.model.User
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherDashboardFragment : Fragment() {
    private var _binding: FragmentTeacherDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeacherDashboardViewModel by viewModels()
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var studentProgressAdapter: StudentProgressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecycleView()
        observeViewModel()
        loadData()
        setupSearch()
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            showLoading(true)
            findNavController().navigate(R.id.action_teacherDashboardFragment_to_editProfileFragment)
        }

        binding.btnManageLessons.setOnClickListener {
            showLoading(true)
            findNavController().navigate(R.id.action_teacherDashboardFragment_to_teacherLessonListFragment)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.searchStudent(
                query = text?.toString() ?: ""
            )
        }
    }

    private fun setupRecycleView() {
        studentAdapter = StudentAdapter { student ->
            val action = TeacherDashboardFragmentDirections
                .actionTeacherDashboardFragmentToStudentProfileFragment(student.id)
            findNavController().navigate(action)
        }
        studentProgressAdapter = StudentProgressAdapter()

        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = studentAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.teacherState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is TeacherDashboardViewModel.TeacherState.Loading,
            is TeacherDashboardViewModel.StudentState.Loading -> {
                showLoading(true)
            }

            is TeacherDashboardViewModel.TeacherState.Success -> {
                showLoading(false)
                updateParentUI(state.teacher)
            }

            is TeacherDashboardViewModel.StudentState.Success -> {
                showLoading(false)
                studentAdapter.submitList(state.students)
            }

            is TeacherDashboardViewModel.TeacherState.Error,
            is TeacherDashboardViewModel.StudentState.Error -> {
                showLoading(false)
                showToast(
                    (state as? TeacherDashboardViewModel.TeacherState.Error)?.message
                        ?: (state as? TeacherDashboardViewModel.StudentState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    private fun loadData() {
        viewModel.loadUserData()
        viewModel.loadAllStudentData()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnEditProfile.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateParentUI(parent: User) {
        binding.apply {
            tvName.text = parent.name
            tvEmail.text = parent.email

            parent.profilePicture.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .placeholder(R.drawable.ic_user)
                    .into(ivProfilePicture)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}