package com.adika.learnable.view.profile

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentEditProfileBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.LanguageUtils
import com.adika.learnable.util.ValidationResult
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.EditProfileViewModel
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri = result.data?.data!!
            viewModel.uploadProfilePicture(uri)
        } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(requireContext(), ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply saved language
        val languageCode = LanguageUtils.getLanguagePreference(requireContext())
        val config = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        setupObservers()
        setupClickListeners()
        viewModel.loadUserProfile()
        updateLanguageIcon()
    }

    private fun setupObservers() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditProfileViewModel.UserState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.contentLayout.visibility = View.GONE
                }
                is EditProfileViewModel.UserState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    state.user?.let { updateUI(it) }
                }
                is EditProfileViewModel.UserState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.contentLayout.visibility = View.VISIBLE
                    showToast(state.message)
                }
                is EditProfileViewModel.UserState.PasswordUpdated -> {
                    showToast(getString(R.string.password_update_success))
                    clearPasswordFields()
                }
            }
        }

        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditProfileViewModel.UploadState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is EditProfileViewModel.UploadState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    // Load gambar baru
                    Glide.with(requireContext())
                        .load(state.imageUrl)
                        .circleCrop()
                        .into(binding.ivProfilePicture)

                }
                is EditProfileViewModel.UploadState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnUpdateProfile.setOnClickListener {
            val user = collectUserData()
            if (validateUserData(user)) {
                viewModel.updateUserProfile(user)
            }
        }

        binding.btnChangePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            if (validatePassword(newPassword)) {
                viewModel.updatePassword(currentPassword, newPassword)
            }
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_editProfile_to_loginFrament)
        }

        binding.btnLanguage.setOnClickListener {
            toggleLanguage()
        }
    }

    private fun openImagePicker() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }


    private fun validateUserData(user: User): Boolean {
        val validationResult = ValidationUtils.validateUserData(
            context = requireContext(),
            user = user
        )

        when (validationResult) {
            is ValidationResult.Invalid -> {
                showToast(validationResult.message)
                return false
            }
            is ValidationResult.Valid -> return true
        }
    }

    private fun validatePassword(password: String): Boolean {
        val validationResult = ValidationUtils.validatePassword(
            context = requireContext(),
            password = password,
            minLength = 8,
            requireUppercase = true,
            requireLowercase = true,
            requireNumber = true,
            requireSpecialChar = true
        )

        when (validationResult) {
            is ValidationResult.Invalid -> {
                showToast(validationResult.message)
                return false
            }
            is ValidationResult.Valid -> return true
        }
    }

    private fun toggleLanguage() {
        val currentLanguage = LanguageUtils.getLanguagePreference(requireContext())
        val newLanguage = if (currentLanguage == "id") "en" else "id"
        
        // Update language
        LanguageUtils.changeLanguage(requireContext(), newLanguage)
        
        // Update UI
        updateLanguageIcon()
        
        // Recreate activity to apply changes
        activity?.recreate()
    }

    private fun updateLanguageIcon() {
        val currentLanguage = LanguageUtils.getLanguagePreference(requireContext())
        val config = resources.configuration
        val locale = Locale(currentLanguage)
        Locale.setDefault(locale)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        val resId = if (currentLanguage == "id") R.drawable.ic_flag_id else R.drawable.ic_flag_usa
        binding.btnLanguage.setImageResource(resId)
    }

    private fun updateUI(user: User) {
        binding.apply {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)

            // Load profile picture
            user.profilePicture.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .placeholder(R.drawable.ic_user)
                    .into(ivProfilePicture)
            }
        }
    }

    private fun collectUserData(): User {
        val currentUser = (viewModel.userState.value as? EditProfileViewModel.UserState.Success)?.user
        return User(
            id = currentUser?.id ?: "",
            name = binding.etName.text.toString(),
            email = binding.etEmail.text.toString(),
            phone = binding.etPhone.text.toString()
        )
    }

    private fun clearPasswordFields() {
        binding.etCurrentPassword.text?.clear()
        binding.etNewPassword.text?.clear()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}