package com.adika.learnable.view.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentMoreBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.loadAvatar
import com.adika.learnable.view.auth.LogoutDialogFragment
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : BaseFragment() {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        loadProfileData()
        setupMenuClickListeners()
        setupDialogListener()

        setupTextScaling()
    }

    private fun setupMenuClickListeners() {

        binding.menuAccount.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_accountProfileFragment)
        }

        binding.menuSaved.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_materialBookmarkFragment)
        }

        binding.menuSettings.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_settingFragment)
        }

        binding.menuAbout.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_aboutFragment)
        }

        binding.menuFeedback.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_feedbackFragment)
        }

        binding.menuLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDialogListener() {
        childFragmentManager.setFragmentResultListener(
            LogoutDialogFragment.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(LogoutDialogFragment.ACTION)) {
                LogoutDialogFragment.ACTION_BACK_TO_LOGIN -> {
                    viewModel.logout()
                    findNavController().navigate(R.id.action_moreFragment_to_login)
                }
            }
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
            profileName.text = user.name
            profileEmail.text = user.email

            profileImage.loadAvatar(
                name = user.name,
                photoUrl = user.profilePicture
            )
        }
    }

    private fun showLogoutConfirmation() {
        LogoutDialogFragment().show(childFragmentManager, LogoutDialogFragment.TAG)
    }

    private fun loadProfileData() {
        val currentState = viewModel.userState.value
        if (currentState is ProfileViewModel.UserState.Success && currentState.user != null) {
            updateUI(currentState.user)
            return
        }
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