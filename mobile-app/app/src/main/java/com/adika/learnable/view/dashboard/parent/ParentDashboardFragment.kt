package com.adika.learnable.view.dashboard.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.StudentAdapter
import com.adika.learnable.adapter.StudentProgressAdapter
import com.adika.learnable.databinding.FragmentParentDashboardBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.ValidationResult
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.dashboard.ParentDashboardViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParentDashboardFragment : Fragment() {
    private var _binding: FragmentParentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var studentProgressAdapter: StudentProgressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerViews() {
        studentAdapter = StudentAdapter { student ->
            val action = ParentDashboardFragmentDirections
                .actionParentDashboardFragmentToStudentProfileFragment(student.id)
            findNavController().navigate(action)
        }
        studentProgressAdapter = StudentProgressAdapter()

        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = studentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            showLoading(true)
            findNavController().navigate(R.id.action_parentDashboardFragment_to_editProfileFragment)
        }

        binding.btnConnectStudent.setOnClickListener {
            val studentEmail = binding.etStudentEmail.text.toString()
            if (validateEmail(studentEmail)) {
                viewModel.connectStudent(studentEmail)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.parentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is ParentDashboardViewModel.StudentState.Loading,
            is ParentDashboardViewModel.ParentState.Loading -> {
                showLoading(true)
            }

            is ParentDashboardViewModel.ParentState.Success -> {
                showLoading(false)
                updateParentUI(state.parent)
            }

            is ParentDashboardViewModel.StudentState.Success -> {
                showLoading(false)
                studentAdapter.submitList(state.students)
            }

            is ParentDashboardViewModel.StudentState.SuccessMessage -> {
                showLoading(false)
                showToast(state.message)
            }

            is ParentDashboardViewModel.StudentState.Error,
            is ParentDashboardViewModel.ParentState.Error -> {
                showLoading(false)
                showToast(
                    (state as? ParentDashboardViewModel.StudentState.Error)?.message
                        ?: (state as? ParentDashboardViewModel.ParentState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    private fun loadData() {
        viewModel.loadUserData()
        viewModel.loadStudents()
    }

    private fun validateEmail(email: String): Boolean {
        val validationResult = ValidationUtils.validateEmail(
            context = requireContext(),
            email = email
        )

        when (validationResult) {
            is ValidationResult.Invalid -> {
                showToast(validationResult.message)
                return false
            }

            is ValidationResult.Valid -> return true
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnConnectStudent.isEnabled = !isLoading
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