package com.adika.learnable.view.auth

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentLoginBinding
import com.adika.learnable.util.ValidationResult
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.auth.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        credentialManager = CredentialManager.create(requireContext())
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInputs(email, password)) {
                return@setOnClickListener
            }

            showLoading(true)
            disableButtons()
            viewModel.loginUser(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            showLoading(true)
            disableButtons()
            launchCredentialManager()
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            handleAuthState(state)
        }

        viewModel.googleSignInState.observe(viewLifecycleOwner) { state ->
            handleAuthState(state)
        }
    }

    private fun handleAuthState(state: Any) {
        when (state) {
            is LoginViewModel.LoginState.Loading,
            is LoginViewModel.GoogleSignInState.Loading -> {
                showLoading(true)
            }

            is LoginViewModel.LoginState.Success,
            is LoginViewModel.GoogleSignInState.Success -> {
                showLoading(false)
            }

            is LoginViewModel.LoginState.Error,
            is LoginViewModel.GoogleSignInState.Error -> {
                showLoading(false)
                showToast(
                    (state as? LoginViewModel.LoginState.Error)?.message
                        ?: (state as? LoginViewModel.GoogleSignInState.Error)?.message
                        ?: "Unknown error"
                )
                enableButtons()
            }

            is LoginViewModel.LoginState.NavigateToDisabilitySelection,
            is LoginViewModel.GoogleSignInState.NavigateToDisabilitySelection -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_disability_selection)
            }

            is LoginViewModel.LoginState.NavigateToStudentDashboard,
            is LoginViewModel.GoogleSignInState.NavigateToStudentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_student_dashboard)
            }

            is LoginViewModel.LoginState.NavigateToTeacherDashboard,
            is LoginViewModel.GoogleSignInState.NavigateToTeacherDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_teacher_dashboard)
            }

            is LoginViewModel.LoginState.NavigateToParentDashboard,
            is LoginViewModel.GoogleSignInState.NavigateToParentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_parent_dashboard)
            }

        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        val validationResult = ValidationUtils.validateLoginData(
            context = requireContext(),
            email = email,
            password = password
        )

        when (validationResult) {
            is ValidationResult.Invalid -> {
                showError(validationResult.message)
                return false
            }

            is ValidationResult.Valid -> return true
        }
    }


    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, getString(R.string.credential_error), e)
                showLoading(false)
                enableButtons()
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Kredensial bukan tipe Google ID!")
            enableButtons()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        binding.apply {
            btnLogin.isEnabled = !isLoading
            btnGoogleSignIn.isEnabled = !isLoading

            // Nonaktifkan/aktifkan semua EditText
            etEmail.isEnabled = !isLoading
            etPassword.isEnabled = !isLoading

            tvSignUp.isEnabled = !isLoading
            tvForgotPassword.isEnabled = !isLoading

            val alpha = if (isLoading) 0.5f else 1.0f
            btnLogin.alpha = alpha
            btnGoogleSignIn.alpha = alpha
            etEmail.alpha = alpha
            etPassword.alpha = alpha
            tvSignUp.alpha = alpha
            tvForgotPassword.alpha = alpha

            if (isLoading) {
                etEmail.clearFocus()
                etPassword.clearFocus()
                val imm =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etEmail.windowToken, 0)
                imm.hideSoftInputFromWindow(etPassword.windowToken, 0)
            }
        }
    }

    private fun enableButtons() {
        binding.btnLogin.isEnabled = true
        binding.btnGoogleSignIn.isEnabled = true
    }

    private fun disableButtons() {
        binding.btnLogin.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}