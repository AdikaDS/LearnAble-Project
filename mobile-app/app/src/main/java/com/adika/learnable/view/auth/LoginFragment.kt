package com.adika.learnable.view.auth

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentLoginBinding
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.auth.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
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
            clearLoginError()

            val isValid = validateInputs()
            if (!isValid) {
                showLoading(false)
                enableButtons()
                return@setOnClickListener
            }

            val email = binding.etEmail.getText().trim()
            val password = binding.etPassword.getText().trim()

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
            handleState(state)
        }

        viewModel.googleSignInState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.navigationState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is LoginViewModel.LoginState.Loading,
            is LoginViewModel.GoogleSignInState.Loading ->
                showLoading(true)

            is LoginViewModel.LoginState.Success,
            is LoginViewModel.GoogleSignInState.Success ->
                showLoading(false)

            is LoginViewModel.GoogleSignInState.NeedMoreData -> {
                showLoading(false)
                val action = LoginFragmentDirections
                    .actionSigninToCompleteAdditionalData(
                        userId = state.user.id,
                        prefillEmail = state.user.email,
                        prefillName = state.user.name

                    )
                findNavController().navigate(action)
            }

            is LoginViewModel.LoginState.Error,
            is LoginViewModel.GoogleSignInState.Error -> {
                showLoading(false)
                val errorMessage = (state as? LoginViewModel.LoginState.Error)?.message
                    ?: (state as? LoginViewModel.GoogleSignInState.Error)?.message
                    ?: getString(R.string.unknown_error)

                showLoginError(errorMessage)
                enableButtons()
            }
            is LoginViewModel.NavigationState.NavigateToStudentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_student_dashboard)
            }
            is LoginViewModel.NavigationState.NavigateToTeacherDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_teacher_dashboard)
            }
            is LoginViewModel.NavigationState.NavigateToParentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_parent_dashboard)
            }
            is LoginViewModel.NavigationState.NavigateToAdminConfirmation -> {
                showLoading(false)
                findNavController().navigate(R.id.action_login_to_admin_confirmation)
            }
        }
    }

    private fun validateInputs(): Boolean {
        val isEmail = binding.etEmail.validateWith {
            ValidationUtils.validateEmail(requireContext(), binding.etEmail.getText())
        }

        val isPassword = binding.etPassword.validateWith {
            ValidationUtils.validatePasswordLogin(requireContext(), binding.etPassword.getText())
        }

        return isEmail && isPassword
    }

    private fun clearLoginError() {
        binding.tvLoginError.visibility = View.GONE
        binding.etEmail.setError(null)
        binding.etPassword.setError(null)
    }

    private fun showLoginError(message: String) {
        binding.tvLoginError.text = message
        binding.tvLoginError.visibility = View.VISIBLE
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
            showLoading(false)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}