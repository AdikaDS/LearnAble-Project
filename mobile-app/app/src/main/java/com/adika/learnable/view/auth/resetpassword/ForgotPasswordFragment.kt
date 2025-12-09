package com.adika.learnable.view.auth.resetpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentForgotPasswordBinding
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.auth.ForgotPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
        dialogListener()
    }

    private fun setupClickListeners() {
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.getText().trim()
            if (validateEmail()) {
                viewModel.resetPassword(email)
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgot_password_to_login)
        }
    }

    private fun observeViewModel() {
        viewModel.resetState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ForgotPasswordViewModel.ResetState.Loading -> showLoading(true)
                is ForgotPasswordViewModel.ResetState.Success -> {
                    showLoading(false)
                    EmailSentDialogFragment
                        .newInstance(state.email)
                        .show(childFragmentManager, EmailSentDialogFragment.TAG)
                }

                is ForgotPasswordViewModel.ResetState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun dialogListener() {
        childFragmentManager.setFragmentResultListener(
            EmailSentDialogFragment.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(EmailSentDialogFragment.ACTION)) {
                "resend" -> viewModel.resendResetEmail()              // panggil ulang
                "change" -> binding.etEmail.requestFocus()            // arahkan ke form email
                "close" -> { findNavController().navigate(R.id.action_forgot_password_to_login) }
            }
        }

    }

    private fun validateEmail(): Boolean {
        val isEmail = binding.etEmail.validateWith {
            ValidationUtils.validateEmail(requireContext(), binding.etEmail.getText())
        }

        return isEmail
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 