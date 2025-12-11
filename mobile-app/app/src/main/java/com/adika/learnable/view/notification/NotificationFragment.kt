package com.adika.learnable.view.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.adapter.NotificationAdapter
import com.adika.learnable.databinding.FragmentNotificationBinding
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationFragment : BaseFragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadNotifications()

        setupTextScaling()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = NotificationAdapter { notification ->

                viewModel.markAsRead(notification.id)
            }
        }
    }

    private fun setupObservers() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            (binding.recyclerViewNotifications.adapter as? NotificationAdapter)?.submitList(
                notifications
            )

            binding.layoutEmpty.visibility =
                if (notifications.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewNotifications.visibility =
                if (notifications.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadNotifications() {
        viewModel.loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}