package com.adika.learnable.view.dashboard.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adika.learnable.R
import com.adika.learnable.databinding.BottomSheetUserDetailBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.NormalizeFirestore
import com.adika.learnable.view.auth.LogoutDialogFragment
import com.adika.learnable.view.dashboard.admin.dialog.ConfirmationApprovalDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUserDetailBinding? = null
    private val binding get() = _binding!!

    private var user: User? = null
    private var onApproveClick: ((User) -> Unit)? = null
    private var onRejectClick: ((User) -> Unit)? = null

    companion object {
        private const val ARG_USER = "arg_user"

        fun newInstance(
            user: User,
            onApproveClick: (User) -> Unit,
            onRejectClick: (User) -> Unit
        ): UserDetailBottomSheet {
            val fragment = UserDetailBottomSheet()
            val args = Bundle()
            args.putParcelable(ARG_USER, user)
            fragment.arguments = args
            fragment.onApproveClick = onApproveClick
            fragment.onRejectClick = onRejectClick
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            user = it.getParcelable(ARG_USER) as? User
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        setupDialogListener()
    }

    private fun setupUI() {
        user?.let { userData ->
            // Set basic information
            val realRole =
                userData.role?.let { NormalizeFirestore.unormalizeRole(requireContext(), it) }
            binding.tvDetailRole.text = realRole
            binding.tvDetailName.text = userData.name
            binding.tvDetailEmail.text = userData.email

            when (userData.role?.lowercase()) {

                "teacher" -> {

                    binding.layoutIdNumber.visibility = View.VISIBLE
                    binding.tvDetailIdNumber.text =
                        userData.idNumber ?: getString(R.string.not_fill_yet)

                    if (!userData.phoneNumber.isNullOrEmpty()) {
                        binding.layoutPhoneNumber.visibility = View.VISIBLE
                        binding.tvDetailPhoneNumber.text = userData.phoneNumber
                    } else {
                        binding.layoutPhoneNumber.visibility = View.GONE
                    }
                }

            }
        }
    }

    private fun showApprovalConfirmation() {
        ConfirmationApprovalDialog().show(childFragmentManager, ConfirmationApprovalDialog.TAG)
    }

    private fun setupDialogListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationApprovalDialog.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(LogoutDialogFragment.ACTION)) {
                ConfirmationApprovalDialog.ACTION_APPROVE -> {
                    user?.let { userData ->
                        onApproveClick?.invoke(userData)
                    }
                }

                ConfirmationApprovalDialog.ACTION_BACK_TO_DASHBOARD -> {
                    dismiss()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnApprove.setOnClickListener {
            showApprovalConfirmation()
        }

        binding.btnReject.setOnClickListener {
            user?.let { userData ->
                onRejectClick?.invoke(userData)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
