package com.adika.learnable.view.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.customview.IconEditTextView
import com.adika.learnable.databinding.FragmentSignupBinding
import com.adika.learnable.util.NormalizeFirestore
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.auth.SignupViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignupFragment : BaseFragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    private companion object {
        private const val TAG = "SignUpFragment"
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

        setupDropdownRole()
        setupClickListeners()
        observeViewModel()

        setupTextScaling()
    }

    private fun setupClickListeners() {
        binding.etPassword.apply {
            setMinAcceptableStrength(
                IconEditTextView.PasswordStrength.MEDIUM
            )
        }

        binding.btnSignUp.setOnClickListener {
            val isValid = validateInputs()
            if (!isValid) {
                showLoading(false)
                enableButtons()
                return@setOnClickListener
            }

            val role = binding.etPickRole.getText().trim()
            val normalizeRole = NormalizeFirestore.normalizeRole(requireContext(), role)
            val name = binding.etFullName.getText().trim()
            val nisn = binding.etNisn.getText().trim()
            val nip = binding.etNip.getText().trim()
            val email = binding.etEmail.getText().trim()
            val password = binding.etPassword.getText().trim()

            showLoading(true)
            disableButtons()

            when (role) {
                getString(R.string.student) -> {
                    viewModel.signUpWithEmail(name, email, password, normalizeRole, nisn)
                }

                getString(R.string.teacher) -> {
                    viewModel.signUpWithEmail(name, email, password, normalizeRole, nip)
                }
            }
        }

        binding.btnGoogleSignUp.setOnClickListener {
            showLoading(true)
            disableButtons()
            launchCredentialManager()
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
    }

    private fun setupDropdownRole() {
        val roles = resources.getStringArray(R.array.roles).toList()

        binding.etPickRole.apply {
            setDropdownItems(roles)
            setOnItemSelectedListener { value, _ ->
                setIcon(roleIconFor(value))
                when (value) {
                    getString(R.string.student) -> showStudentForm()
                    getString(R.string.teacher) -> showTeacherForm()
                }
            }

            val current = getText().trim()
            if (current.isNotEmpty()) {
                setIcon(roleIconFor(current))
            }
        }
    }

    private fun validateInputs(): Boolean {
        val isRole = binding.etPickRole.validateWith {
            ValidationUtils.validateRole(requireContext(), binding.etPickRole.getText())
        }

        val isName = binding.etFullName.validateWith {
            ValidationUtils.validateName(requireContext(), binding.etFullName.getText())
        }

        val isNisn = if (binding.etNisn.isVisible) {
            binding.etNisn.validateWith {
                ValidationUtils.validateNISN(requireContext(), binding.etNisn.getText())
            }
        } else true

        val isNip = if (binding.etNip.isVisible) {
            binding.etNip.validateWith {
                ValidationUtils.validateNIP(requireContext(), binding.etNip.getText())
            }
        } else true

        val isEmail = binding.etEmail.validateWith {
            ValidationUtils.validateEmail(requireContext(), binding.etEmail.getText())
        }

        val isConfirmPassword = binding.etConfirmPassword.validateWith {
            ValidationUtils.validateConfirmPassword(
                requireContext(),
                binding.etPassword.getText(),
                binding.etConfirmPassword.getText()
            )
        }

        return isRole && isName && isNisn && isNip && isEmail && isConfirmPassword
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
            is SignupViewModel.GoogleSignUpState.Loading
                -> showLoading(true)

            is SignupViewModel.SignupState.Success -> {
                showLoading(false)
                showToast(getString(R.string.signup_success))
                findNavController().navigate(R.id.action_signup_to_login)

            }

            is SignupViewModel.GoogleSignUpState.Success -> {
                showLoading(false)
            }

            is SignupViewModel.GoogleSignUpState.NeedMoreData -> {
                showLoading(false)
                val action = SignupFragmentDirections
                    .actionSignupToCompleteAdditionalData(
                        userId = state.user.id,
                        prefillEmail = state.user.email,
                        prefillName = state.user.name

                    )
                findNavController().navigate(action)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToStudentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_signup_to_student_dashboard)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToTeacherDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_signup_to_teacher_dashboard)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToAdminConfirmation -> {
                showLoading(false)
                findNavController().navigate(R.id.action_signup_to_admin_confirmation)
            }

            is SignupViewModel.SignupState.Error,
            is SignupViewModel.GoogleSignUpState.Error,
                -> {
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
            viewModel.signUpWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Kredensial bukan tipe Google ID!")
            showLoading(false)
            enableButtons()
        }
    }

    private fun roleIconFor(value: String): Int {
        return when (value) {
            getString(R.string.student) -> R.drawable.ic_student
            getString(R.string.teacher) -> R.drawable.ic_teacher
            else -> R.drawable.ic_role
        }
    }

    private fun resetValue() {
        binding.etFullName.reset()
        binding.etEmail.reset()
        binding.etPassword.reset()
        binding.etConfirmPassword.reset()
        binding.etNip.reset()
        binding.etNisn.reset()
    }

    private fun showStudentForm() {
        binding.etFullName.visibility = View.VISIBLE
        binding.etEmail.visibility = View.VISIBLE
        binding.etPassword.visibility = View.VISIBLE
        binding.etConfirmPassword.visibility = View.VISIBLE

        binding.etNisn.visibility = View.VISIBLE
        binding.etNip.visibility = View.GONE
        resetValue()
    }

    private fun showTeacherForm() {
        binding.etFullName.visibility = View.VISIBLE
        binding.etEmail.visibility = View.VISIBLE
        binding.etPassword.visibility = View.VISIBLE
        binding.etConfirmPassword.visibility = View.VISIBLE

        binding.etNisn.visibility = View.GONE
        binding.etNip.visibility = View.VISIBLE
        resetValue()
    }


    private fun enableButtons() {
        binding.btnSignUp.isEnabled = true
        binding.btnGoogleSignUp.isEnabled = true
    }

    private fun disableButtons() {
        binding.btnSignUp.isEnabled = false
        binding.btnGoogleSignUp.isEnabled = false
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
        binding.btnGoogleSignUp.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}