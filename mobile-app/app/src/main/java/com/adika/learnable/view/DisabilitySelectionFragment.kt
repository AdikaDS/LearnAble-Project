package com.adika.learnable.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.adika.learnable.databinding.FragmentDisabilitySelectionBinding
import com.adika.learnable.viewmodel.DisabilitySelectionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisabilitySelectionFragment : Fragment() {
    private var _binding: FragmentDisabilitySelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DisabilitySelectionViewModel by viewModels()

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
            viewModel.saveDisabilityType("tunarungu")
        }

        binding.tunanetraCard.setOnClickListener {
            viewModel.saveDisabilityType("tunanetra")
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(requireActivity()) { state ->
            when (state) {
                is DisabilitySelectionViewModel.DisabilitySelectionState.Loading -> {
                    showLoading(true)
                }

                is DisabilitySelectionViewModel.DisabilitySelectionState.Success -> {
                    showLoading(false)
                    showToast("Berhasil menyimpan tipe disabilitas")
                    // Navigate to next screen with state.user data
                }

                is DisabilitySelectionViewModel.DisabilitySelectionState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

    }

//    private fun navigateToDashboard(role: String) {
//        val intent = when (role) {
//            "teacher" -> Intent(this, TeacherDashboardActivity::class.java)
//            "parent" -> Intent(this, ParentDashboardActivity::class.java)
//            else -> Intent(this, StudentDashboardActivity::class.java)
//        }
//        startActivity(intent)
//        finish()
//    }

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