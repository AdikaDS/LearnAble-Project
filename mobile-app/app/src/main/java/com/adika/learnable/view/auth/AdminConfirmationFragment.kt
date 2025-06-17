package com.adika.learnable.view.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentAdminConfirmationBinding
import com.adika.learnable.viewmodel.auth.AdminConfirmationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminConfirmationFragment : Fragment() {
    private var _binding: FragmentAdminConfirmationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminConfirmationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListener()
        viewModel.checkApprovalStatus()
    }

    private fun setupClickListener() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_adminConfirmation_to_login)
        }
    }
    private fun setupObservers() {
        viewModel.approvalState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminConfirmationViewModel.ApprovalState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogout.isEnabled = false
                }
                is AdminConfirmationViewModel.ApprovalState.Approved -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogout.isEnabled = true
                    navigateToDashboard()
                }
                is AdminConfirmationViewModel.ApprovalState.NotApproved -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogout.isEnabled = true
                    binding.tvStatus.text = getString(R.string.waiting_admin_approval)
                }
                is AdminConfirmationViewModel.ApprovalState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogout.isEnabled = true
                    binding.tvStatus.text = state.message
                }
            }
        }
    }

    private fun navigateToDashboard() {
        when (viewModel.getUserRole()) {
            "teacher" -> findNavController().navigate(R.id.action_adminConfirmation_to_teacher_dashboard)
            "parent" -> findNavController().navigate(R.id.action_adminConfirmation_to_parent_dashboard)
            else -> findNavController().navigate(R.id.action_adminConfirmation_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 