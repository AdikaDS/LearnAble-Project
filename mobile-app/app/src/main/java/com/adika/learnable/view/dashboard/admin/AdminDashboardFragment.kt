package com.adika.learnable.view.dashboard.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.ApplicationUserAdapter
import com.adika.learnable.databinding.FragmentAdminDashboardBinding
import com.adika.learnable.model.User
import com.adika.learnable.view.auth.LogoutDialogFragment
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.admin.dialog.SortFilterDialog
import com.adika.learnable.viewmodel.dashboard.AdminDashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardFragment : BaseFragment() {
    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminDashboardViewModel by viewModels()
    private lateinit var adapter: ApplicationUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
        setupDialogListener()

        setupTextScaling()
    }

    private fun setupRecyclerView() {
        adapter = ApplicationUserAdapter(onDetailClick = { item ->
            showUserDetailDialog(item)
        })

        binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAccounts.setHasFixedSize(true)
        binding.rvAccounts.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.ivLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnRole.setOnClickListener {
            showRoleFilterDialog()
        }

        binding.btnStatus.setOnClickListener {
            showStatusFilterDropdown()
        }

        binding.btnSort.setOnClickListener {
            showSortFilterDialog()
        }
    }

    private fun setupDialogListener() {
        childFragmentManager.setFragmentResultListener(
            LogoutDialogFragment.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(LogoutDialogFragment.ACTION)) {
                LogoutDialogFragment.ACTION_BACK_TO_LOGIN -> {
                    viewModel.logout()
                    findNavController().navigate(R.id.action_adminDashboard_to_login)
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        LogoutDialogFragment().show(childFragmentManager, LogoutDialogFragment.TAG)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchUsers(text.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminDashboardViewModel.UserState.Loading -> {

                }

                is AdminDashboardViewModel.UserState.Success -> {
                    adapter.submitList(state.users)
                }

                is AdminDashboardViewModel.UserState.Error -> {
                    showToast(state.message)
                }
            }
        }
    }

    private fun showRoleFilterDialog() {
        val roleOptions = viewModel.getRoleFilterOptions()
        val currentRole = viewModel.selectedRoleFilter.value ?: "all"
        val currentIndex = when (currentRole) {
            "all" -> 0
            "parent" -> 1
            "teacher" -> 2
            else -> 0
        }

        val popup = ListPopupWindow(requireContext())

        val adapter = object : ArrayAdapter<String>(requireContext(), 0, roleOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_dropdown, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvText)

                val activated = position == currentIndex
                textView.isActivated = activated
                textView.text = getItem(position)

                textView.setTextColor(
                    if (activated) Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }

        popup.setAdapter(adapter)
        popup.anchorView = binding.btnRole
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_dropdown_panel
            )
        )

        binding.btnRole.post {

            val paint = binding.btnRole.paint
            var maxWidth = 0
            for (option in roleOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }

            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding

            if (popup.width < binding.btnRole.width) {
                popup.width = binding.btnRole.width
            }

            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val selectedRole = when (position) {
                0 -> "all"
                1 -> "parent"
                2 -> "teacher"
                else -> "all"
            }
            viewModel.filterByRole(selectedRole)
            updateRoleButtonText(roleOptions[position])
            popup.dismiss()
        }
    }

    private fun showStatusFilterDropdown() {
        val statusOptions = viewModel.getStatusFilterOptions()
        val currentStatus = viewModel.selectedStatusFilter.value
        val currentIndex = when (currentStatus) {
            null -> 0
            true -> 1
            false -> 2
        }

        val popup = ListPopupWindow(requireContext())

        val adapter = object : ArrayAdapter<String>(requireContext(), 0, statusOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_dropdown, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvText)

                val activated = position == currentIndex
                textView.isActivated = activated
                textView.text = getItem(position)

                textView.setTextColor(
                    if (activated) Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }

        popup.setAdapter(adapter)
        popup.anchorView = binding.btnStatus
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_dropdown_panel
            )
        )

        binding.btnStatus.post {

            val paint = binding.btnStatus.paint
            var maxWidth = 0
            for (option in statusOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }

            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding

            if (popup.width < binding.btnStatus.width) {
                popup.width = binding.btnStatus.width
            }

            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = when (position) {
                0 -> null
                1 -> true
                2 -> false
                else -> null
            }
            viewModel.filterByStatus(selectedStatus)
            updateStatusButtonText(statusOptions[position])
            popup.dismiss()
        }
    }

    private fun updateRoleButtonText(text: String) {
        binding.btnRole.text = text
    }

    private fun updateStatusButtonText(text: String) {
        binding.btnStatus.text = text
    }

    private fun showUserDetailDialog(user: User) {
        val bottomSheet = UserDetailBottomSheet.newInstance(
            user = user,
            onApproveClick = { userData ->
                handleApproveUser(userData)
            },
            onRejectClick = { userData ->
                handleRejectUser(userData)
            }
        )
        bottomSheet.show(parentFragmentManager, "UserDetailBottomSheet")
    }

    private fun handleApproveUser(user: User) {
        viewModel.approveUser(user.id)
    }

    private fun handleRejectUser(user: User) {
        viewModel.rejectUser(user.id)
    }

    private fun showSortFilterDialog() {
        val currentOptions = viewModel.sortFilterOptions.value
        val dialog = SortFilterDialog.newInstance(
            currentOptions = currentOptions,
            onSortFilterApplied = { options ->
                viewModel.applySortFilter(options)
            }
        )
        dialog.show(parentFragmentManager, "SortFilterDialog")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}