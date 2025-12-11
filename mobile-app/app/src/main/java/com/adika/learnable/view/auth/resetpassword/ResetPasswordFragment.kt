package com.adika.learnable.view.auth.resetpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adika.learnable.R
import com.adika.learnable.customview.IconEditTextView
import com.adika.learnable.databinding.FragmentResetPasswordBinding
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.view.auth.resetpassword.dialog.ResetPasswordSuccessDialogFragment
import com.adika.learnable.viewmodel.auth.ResetPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {
    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResetPasswordViewModel by viewModels()
    private val args: ResetPasswordFragmentArgs by navArgs()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentResetPasswordBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeViewModel()
        setupClickListeners()
        setupDialogListener()
    }

    private fun setupDialogListener() {
        childFragmentManager.setFragmentResultListener(
            ResetPasswordSuccessDialogFragment.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(ResetPasswordSuccessDialogFragment.ACTION)) {
                ResetPasswordSuccessDialogFragment.ACTION_BACK_TO_LOGIN, ResetPasswordSuccessDialogFragment.ACTION_CLOSE -> {

                    findNavController().navigate(R.id.action_reset_password_to_login)
                }
            }
        }
    }

    private fun setupClickListeners() {
        val oob = args.oobCode
        if (oob.isBlank()) {
            showToast("Link reset tidak valid")
            findNavController().popBackStack(); return
        }
        viewModel.verifyResetCode(oob)

        binding.etPassword.apply {
            setMinAcceptableStrength(
                IconEditTextView.PasswordStrength.MEDIUM
            )
        }

        binding.btnResetPassword.setOnClickListener {
            val password = binding.etPassword.getText().trim()

            val isValid = validateInputs()
            if (!isValid) {
                showLoading(false)
                return@setOnClickListener
            }

            showLoading(true)
            viewModel.confirmPassword(oob, password)
        }

    }

    private fun observeViewModel() {
        viewModel.verifyState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResetPasswordViewModel.VerifyState.Loading -> showLoading(true)
                is ResetPasswordViewModel.VerifyState.Success -> {
                    showLoading(false)
                }
                is ResetPasswordViewModel.VerifyState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.confirmState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResetPasswordViewModel.ConfirmState.Loading -> showLoading(true)
                is ResetPasswordViewModel.ConfirmState.Success -> {
                    showLoading(false)

                    ResetPasswordSuccessDialogFragment()
                        .show(childFragmentManager, ResetPasswordSuccessDialogFragment.TAG)
                }
                is ResetPasswordViewModel.ConfirmState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val isConfirmPassword = binding.etConfirmPassword.validateWith {
            ValidationUtils.validateConfirmPassword(
                requireContext(),
                binding.etPassword.getText(),
                binding.etConfirmPassword.getText()
            )
        }

        return isConfirmPassword
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !loading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView(); _binding = null
    }
}