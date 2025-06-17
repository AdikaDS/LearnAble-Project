package com.adika.learnable.view.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.adika.learnable.databinding.FragmentSignupBinding
import com.adika.learnable.util.ValidationResult
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.view.SelectRoleBottomSheet
import com.adika.learnable.viewmodel.auth.SignupViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private companion object {
        private const val TAG = "SignUpFragment"
        private const val MIN_PASSWORD_LENGTH = 8
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        credentialManager = CredentialManager.create(requireContext())

        if (savedInstanceState == null && viewModel.role == null) {
            showToast("Silakan pilih peran terlebih dahulu")
            showRoleBottomSheet { selectedRole ->
                viewModel.setRole(selectedRole)
            }
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.etTtl.setOnClickListener {
            showDatePicker()
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val ttl = binding.etTtl.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (!validateInputs(name, email, password, confirmPassword)) {
                return@setOnClickListener
            }

            showLoading(true)
            disableButtons()
            if (viewModel.role != null) {
                viewModel.signUpWithEmail(name, email, password, ttl, viewModel.role!!)
            } else {
                showToast("Silakan pilih peran terlebih dahulu")
                showRoleBottomSheet { selectedRole ->
                    viewModel.setRole(selectedRole)
                }
            }

        }

        binding.btnGoogleSignIn.setOnClickListener {
            showLoading(true)
            disableButtons()
            launchCredentialManager()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                binding.etTtl.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.maxDate =
            System.currentTimeMillis() // tidak bisa pilih tanggal di masa depan
        datePicker.show()
    }

    private fun showRoleBottomSheet(onRoleSelected: (String) -> Unit) {
        val sheet = SelectRoleBottomSheet(onRoleSelected)
        sheet.show(parentFragmentManager, "SelectRoleBottomSheet")
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        val validationResult = ValidationUtils.validateSignupData(
            context = requireContext(),
            name = name,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            minPasswordLength = MIN_PASSWORD_LENGTH
        )

        when (validationResult) {
            is ValidationResult.Invalid -> {
                showToast(validationResult.message)
                return false
            }

            is ValidationResult.Valid -> return true
        }
    }

    private fun observeViewModel() {
        viewModel.signupState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.googleSignUpState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is SignupViewModel.SignupState.Loading,
            is SignupViewModel.GoogleSignUpState.Loading -> showLoading(true)

            is SignupViewModel.SignupState.Success -> {
                showLoading(false)
                showToast(getString(R.string.signup_success))
                findNavController().navigate(R.id.action_signup_to_login)
            }

            is SignupViewModel.GoogleSignUpState.Success -> {
                showLoading(false)
                showToast(getString(R.string.signup_success))
                when (viewModel.role) {
                    "student" -> findNavController().navigate(R.id.action_signup_to_disability_selection)
                    "teacher", "parent" -> findNavController().navigate(R.id.action_signup_to_login)
                    else -> findNavController().navigate(R.id.action_signup_to_login)
                }
            }

            is SignupViewModel.SignupState.Error,
            is SignupViewModel.GoogleSignUpState.Error -> {
                showLoading(false)
                showToast(
                    (state as? SignupViewModel.SignupState.Error)?.message
                        ?: (state as? SignupViewModel.GoogleSignUpState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
                enableButtons()
            }
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
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            if (viewModel.role != null) {
                viewModel.signUpWithGoogle(googleIdTokenCredential.idToken, viewModel.role!!)
            } else {
                showToast("Silakan pilih peran terlebih dahulu")
                showRoleBottomSheet { selectedRole ->
                    viewModel.setRole(selectedRole)
                }
            }


        } else {
            Log.w(TAG, "Kredensial bukan tipe Google ID!")
        }
    }

    private fun enableButtons() {
        binding.btnSignUp.isEnabled = true
        binding.btnGoogleSignIn.isEnabled = true
    }

    private fun disableButtons() {
        binding.btnSignUp.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 