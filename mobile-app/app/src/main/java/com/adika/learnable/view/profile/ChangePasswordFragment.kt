package com.adika.learnable.view.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentChangePasswordBinding
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : BaseFragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var minAcceptableStrength = PasswordStrength.MEDIUM
    private var isProcessing = false

    private companion object {
        private val WEAK = R.string.weak
        private val MEDIUM = R.string.medium
        private val STRONG = R.string.strong
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()

        updateStrengthUI(calculateStrength(""))

        binding.NewPasswordEditText.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                val pw = text?.toString().orEmpty()
                val strength = calculateStrength(pw)
                updateStrengthUI(strength)
                updateSubmitEnabled()
            },
            afterTextChanged = { _ -> }
        )

        binding.RepeatNewPasswordEditText.addTextChangedListener(
            onTextChanged = { _, _, _, _ -> updateSubmitEnabled() },
            afterTextChanged = { _ -> }
        )

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.PreviousPasswordEditText.text?.toString().orEmpty()
            val newPassword = binding.NewPasswordEditText.text?.toString().orEmpty()
            viewModel.updatePassword(currentPassword, newPassword)
        }

        setupTextScaling()
    }

    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ProfileViewModel.UserState.Loading -> showLoading(true)
                is ProfileViewModel.UserState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }

                ProfileViewModel.UserState.PasswordUpdated -> {
                    showLoading(false)
                    showToast(getString(R.string.password_update_success))
                    findNavController().popBackStack(R.id.accountProfileFragment, false)
                }

                is ProfileViewModel.UserState.Success -> showLoading(false)
                else -> Unit
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        isProcessing = isLoading
        binding.progressBar.isVisible = isLoading
        binding.btnChangePassword.text =
            getString(if (isLoading) R.string.processing else R.string.save)
        updateSubmitEnabled()
    }


    enum class PasswordStrength(@StringRes val labelRes: Int, val segments: Int, @ColorRes val colorRes: Int) {
        EMPTY(0, 0, android.R.color.transparent),
        WEAK(ChangePasswordFragment.WEAK, 1, R.color.strength_weak),
        MEDIUM(ChangePasswordFragment.MEDIUM, 2, R.color.strength_medium),
        STRONG(ChangePasswordFragment.STRONG, 3, R.color.strength_strong)
    }

    private fun calculateStrength(pw: String): PasswordStrength {
        if (pw.isBlank()) return PasswordStrength.EMPTY

        var score = 0
        val length = pw.length
        val hasLower = pw.any { it.isLowerCase() }
        val hasUpper = pw.any { it.isUpperCase() }
        val hasDigit = pw.any { it.isDigit() }
        val hasSymbol = pw.any { !it.isLetterOrDigit() }

        if (length >= 8) score++
        if (length >= 12) score++
        if (hasLower) score++
        if (hasUpper) score++
        if (hasDigit) score++
        if (hasSymbol) score++

        val allSame = pw.toSet().size == 1
        val onlyLetters = pw.all { it.isLetter() }
        val onlyDigits = pw.all { it.isDigit() }
        if (allSame || onlyLetters || onlyDigits) score = maxOf(1, score - 2)

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score in 3..4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    private fun updateStrengthUI(strength: PasswordStrength) {
        if (strength == PasswordStrength.EMPTY) {
            binding.strengthGroup.visibility = View.GONE
            return
        }
        binding.strengthGroup.visibility = View.VISIBLE

        val context = requireContext()
        val color = ContextCompat.getColor(context, strength.colorRes)
        val gray = ContextCompat.getColor(context, android.R.color.darker_gray)

        binding.bar1.setBackgroundColor(if (strength.segments >= 1) color else gray)
        binding.bar2.setBackgroundColor(if (strength.segments >= 2) color else gray)
        binding.bar3.setBackgroundColor(if (strength.segments >= 3) color else gray)

        binding.tvStrength.text = context.getString(strength.labelRes)
        binding.tvStrength.setTextColor(color)
    }

    private fun updateSubmitEnabled() {
        val newPw = binding.NewPasswordEditText.text?.toString().orEmpty()
        val repeatPw = binding.RepeatNewPasswordEditText.text?.toString().orEmpty()
        val strength = calculateStrength(newPw)
        val strengthOk = strength.ordinal >= minAcceptableStrength.ordinal
        val repeatOk = repeatPw.isNotEmpty() && repeatPw == newPw
        binding.btnChangePassword.isEnabled = !isProcessing && strengthOk && repeatOk
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}