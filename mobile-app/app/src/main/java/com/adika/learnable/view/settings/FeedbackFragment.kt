package com.adika.learnable.view.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.BuildConfig
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentFeedbackBinding
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.FeedbackViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackFragment : BaseFragment() {
    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedbackViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryDropdown()
        setupRatings()
        setupTextWatchers()
        setupObservers()
        onClickListener()
        updateSubmitEnabled()

        setupTextScaling()
    }

    private fun setupCategoryDropdown() {
        val categories = listOf(
            getString(R.string.bug_feedback),
            getString(R.string.uiux_feedback),
            getString(R.string.content_feedback),
            getString(R.string.other_feedback)
        )
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categoryEditText.setAdapter(adapter)
        binding.categoryEditText.setOnClickListener {
            binding.categoryEditText.showDropDown()
        }
    }

    private fun setupRatings() {
        binding.ratingBar.setOnRatingBarChangeListener { _, _, _ ->
            updateSubmitEnabled()
        }
    }

    private fun setupTextWatchers() {
        binding.subjectEditText.addTextChangedListener { updateSubmitEnabled() }
        binding.messageEditText.addTextChangedListener { updateSubmitEnabled() }
        binding.categoryEditText.addTextChangedListener { updateSubmitEnabled() }
    }

    private fun isFormValid(): Boolean {
        val subjectLen = binding.subjectEditText.text?.length ?: 0
        val messageLen = binding.messageEditText.text?.length ?: 0
        val category = binding.categoryEditText.text?.toString()?.trim().orEmpty()
        val ratingSelected = binding.ratingBar.rating > 0f
        val subjectOk = subjectLen in 1..100
        val messageOk = messageLen in 1..500
        val categoryOk = category.isNotEmpty()
        return subjectOk && messageOk && ratingSelected && categoryOk
    }

    private fun updateSubmitEnabled() {
        val isLoading = viewModel.isSubmitting.value == true
        binding.btnSubmit.isEnabled = !isLoading && isFormValid()
    }

    private fun setupObservers() {
        viewModel.isSubmitting.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSubmit.text = if (isLoading) "Mengirim..." else "Kirim Feedback"
            updateSubmitEnabled()
        }

        viewModel.submitSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(
                    requireContext(),
                    "Terima kasih atas feedback Anda!",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
                viewModel.resetState()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onClickListener() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            val subject = binding.subjectEditText.text?.toString()?.trim() ?: ""
            val category = binding.categoryEditText.text?.toString()?.trim() ?: ""
            val message = binding.messageEditText.text?.toString()?.trim() ?: ""
            val rating =
                if (binding.ratingBar.rating > 0) binding.ratingBar.rating.toInt() else null
            val appVersion = BuildConfig.VERSION_NAME
            val device = "${Build.MANUFACTURER} ${Build.MODEL} / ${Build.VERSION.RELEASE}"

            viewModel.submit(subject, category, message, rating, appVersion, device)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}