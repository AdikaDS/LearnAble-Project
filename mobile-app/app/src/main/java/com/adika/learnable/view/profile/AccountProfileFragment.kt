package com.adika.learnable.view.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentAccountProfileBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.loadAvatar
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountProfileFragment : BaseFragment() {
    private var _binding: FragmentAccountProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        loadProfileData()
        setOnclickListener()

        setupTextScaling()
    }

    private fun setOnclickListener() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_accountProfileFragment_to_editProfileFragment)
        }

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_accountProfileFragment_to_changePasswordFragment)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is ProfileViewModel.UserState.Loading -> {
                showLoading(true)
            }

            is ProfileViewModel.UserState.Success -> {
                showLoading(false)
                state.user?.let { updateUI(it) }
            }

            is ProfileViewModel.UserState.Error -> {
                showLoading(false)
                showToast(state.message)
            }
        }
    }

    private fun updateUI(user: User) {
        binding.apply {
            tvName.text = user.name
            tvEmail.text = user.email
            if (user.phoneNumber.isNullOrEmpty()) {
                tvPhone.visibility = View.GONE
            } else {
                tvPhone.visibility = View.VISIBLE
                tvPhone.text = getString(R.string.phone_id_format, user.phoneNumber)
            }

            if (user.studentData.grade.isEmpty()) {
                tvClass.visibility = View.GONE
            } else {
                tvClass.visibility = View.VISIBLE
                tvClass.text = user.studentData.grade
            }

            profileImage.loadAvatar(
                name = user.name,
                photoUrl = user.profilePicture
            )

            tvStreet.text = user.studentData.address
            tvCity.text = user.studentData.cityAddress
            tvProvince.text = user.studentData.provinceAddress

            tvParentName.text = user.studentData.nameParent
            tvParentNumber.text = user.studentData.phoneNumberParent
        }
    }

    private fun loadProfileData() {
        viewModel.loadUserProfile()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}