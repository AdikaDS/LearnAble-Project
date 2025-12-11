package com.adika.learnable.view.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentCompleteAdditionalDataBinding
import com.adika.learnable.util.NormalizeFirestore
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.auth.SignupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CompleteAdditionalDataFragment : Fragment() {
    private var _binding: FragmentCompleteAdditionalDataBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by viewModels()

    private val args: CompleteAdditionalDataFragmentArgs by navArgs()

    private companion object {
        private const val STUDENT = "student"
        private const val TEACHER = "teacher"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompleteAdditionalDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdownRole()
        setupClickListeners()
        observeViewModel()

        prefillEditText()
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

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val isValid = validateInputs()
            if (!isValid) {
                showLoading(false)
                enableButtons()
                return@setOnClickListener
            }

            val role = binding.etPickRole.getText().trim()
            val normalizeRole = NormalizeFirestore.normalizeRole(requireContext(), role)
            val nomorInduk = when (normalizeRole) {
                STUDENT -> binding.etNisn.getText().trim()
                TEACHER -> binding.etNip.getText().trim()
                else -> null
            }

            showLoading(true)
            disableButtons()

            viewModel.completeAdditionalData(
                uid = args.userId,
                role = normalizeRole,
                idNumber = nomorInduk
            )
        }
    }

    private fun validateInputs(): Boolean {
        val isRole = binding.etPickRole.validateWith {
            ValidationUtils.validateRole(requireContext(), binding.etPickRole.getText())
        }

        val isName = binding.etFullName.validateWith {
            ValidationUtils.validateName(requireContext(), binding.etFullName.getText())
        }

        val isEmail = binding.etEmail.validateWith {
            ValidationUtils.validateEmail(requireContext(), binding.etEmail.getText())
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

        return isRole && isName && isEmail && isNisn && isNip
    }

    private fun observeViewModel() {
        viewModel.googleSignUpState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is SignupViewModel.GoogleSignUpState.Loading -> showLoading(true)

            is SignupViewModel.GoogleSignUpState.Success -> {
                showLoading(false)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToStudentDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_complete_to_student_dashboard)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToTeacherDashboard -> {
                showLoading(false)
                findNavController().navigate(R.id.action_complete_to_teacher_dashboard)
            }

            is SignupViewModel.GoogleSignUpState.NavigateToAdminConfirmation -> {
                showLoading(false)
                findNavController().navigate(R.id.action_complete_to_admin_confirmation)
            }

            is SignupViewModel.GoogleSignUpState.Error -> {
                showLoading(false)
                showToast(
                    (state as? SignupViewModel.GoogleSignUpState.Error)?.message ?: getString(
                        R.string.unknown_error
                    )
                )
                enableButtons()
            }
        }
    }

    private fun enableButtons() {
        binding.btnSignUp.isEnabled = true
    }

    private fun roleIconFor(value: String): Int {
        return when (value) {
            getString(R.string.student) -> R.drawable.ic_student
            getString(R.string.teacher) -> R.drawable.ic_teacher
            else -> R.drawable.ic_role
        }
    }

    private fun resetValue() {
        binding.etNip.reset()
        binding.etNisn.reset()
    }

    private fun showStudentForm() {
        binding.ivLogin.setImageResource(R.drawable.icon_signup_student)
        binding.etFullName.visibility = View.VISIBLE
        binding.etEmail.visibility = View.VISIBLE
        binding.etNisn.visibility = View.VISIBLE
        binding.etNip.visibility = View.GONE
        resetValue()
    }

    private fun showTeacherForm() {
        binding.ivLogin.setImageResource(R.drawable.icon_signup_teacher)
        binding.etFullName.visibility = View.VISIBLE
        binding.etEmail.visibility = View.VISIBLE
        binding.etNisn.visibility = View.GONE
        binding.etNip.visibility = View.VISIBLE
        resetValue()
    }

    private fun disableButtons() {
        binding.btnSignUp.isEnabled = false
    }

    private fun prefillEditText() {
        binding.etFullName.setText(args.prefillName)
        binding.etEmail.setText(args.prefillEmail)

        binding.etFullName.isFocusable = false
        binding.etEmail.isFocusable = false
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}