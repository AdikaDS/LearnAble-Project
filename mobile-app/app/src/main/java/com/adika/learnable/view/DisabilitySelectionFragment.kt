package com.adika.learnable.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentDisabilitySelectionBinding
import com.adika.learnable.viewmodel.DisabilitySelectionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisabilitySelectionFragment : Fragment() {
    private var _binding: FragmentDisabilitySelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DisabilitySelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prevent going back to login
                requireActivity().finish()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisabilitySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.tunarunguCard.setOnClickListener {
            viewModel.saveDisabilityType("Tunarungu")
        }

        binding.tunanetraCard.setOnClickListener {
            viewModel.saveDisabilityType("Tunanetra")
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DisabilitySelectionViewModel.DisabilitySelectionState.Loading -> {
                    showLoading(true)
                }

                is DisabilitySelectionViewModel.DisabilitySelectionState.Success -> {
                    showLoading(false)
                    showToast(getString(R.string.succes_save_disability))
                    findNavController().navigate(R.id.action_disability_selection_to_student_dashboard)
                }

                is DisabilitySelectionViewModel.DisabilitySelectionState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tunanetraCard.isEnabled = !isLoading
        binding.tunarunguCard.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 