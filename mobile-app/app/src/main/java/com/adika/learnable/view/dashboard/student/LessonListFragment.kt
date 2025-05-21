package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.adapter.LessonAdapter
import com.adika.learnable.databinding.FragmentLessonListBinding
import com.adika.learnable.viewmodel.LessonViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LessonListFragment : Fragment() {
    private var _binding: FragmentLessonListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()
    private lateinit var lessonAdapter: LessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        val idSubject = arguments?.getString("idSubject")

        val disabilityType = arguments?.getString("disabilityType")

        if (idSubject != null && disabilityType != null) {
            viewModel.getLessonsBySubjectAndDisabilityType(idSubject, disabilityType)
        } else {
            showToast("Data tidak lengkap")
        }
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter { lesson ->
            // Navigate to lesson detail
            val action = LessonListFragmentDirections
                .actionLessonListToLessonDetail(lesson.id)
            findNavController().navigate(action)
        }

        binding.lessonsRecyclerView.apply {
            adapter = lessonAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.searchLessons(
                query = text?.toString() ?: "",
                disabilityType = arguments?.getString("disabilityType"),
                idSubject = arguments?.getString("idSubject")
            )
        }
    }

    private fun observeViewModel() {
        viewModel.lessons.observe(viewLifecycleOwner) { lessons ->
            lessonAdapter.submitList(lessons)
            binding.emptyStateText.visibility = if (lessons.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LessonViewModel.LessonState.Loading -> {
                    showLoading(true)
                }
                is LessonViewModel.LessonState.Success -> {
                    showLoading(false)
                }
                is LessonViewModel.LessonState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
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