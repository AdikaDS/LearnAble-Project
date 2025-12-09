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
import com.adika.learnable.R
import com.adika.learnable.adapter.StudentLessonAdapter
import com.adika.learnable.databinding.FragmentLessonListBinding
import com.adika.learnable.viewmodel.LessonViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentLessonListFragment : Fragment() {
    private var _binding: FragmentLessonListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()
    private lateinit var studentLessonAdapter: StudentLessonAdapter

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
        getLesson()
    }

    private fun setupRecyclerView() {
        studentLessonAdapter = StudentLessonAdapter(
            onLessonClick = { lesson ->
                viewModel.loadSubBabsForLessonStudent(lesson.id)
            },
            onSubBabClick = { subBab ->
                val action = StudentLessonListFragmentDirections
                    .actionStudentLessonListFragmentToStudentSubBabDetailFragment(subBab)
                findNavController().navigate(action)
            }
        )

        binding.lessonsRecyclerView.apply {
            adapter = studentLessonAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.searchLessons(
                query = text?.toString() ?: "",
                idSubject = arguments?.getString("idSubject")!!
            )
        }
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LessonViewModel.StudentState.Loading -> {
                    showLoading(true)
                }
                is LessonViewModel.StudentState.Success -> {
                    showLoading(false)
                    state.lessons.let { lessons ->
                        studentLessonAdapter.submitList(lessons)
                        binding.lessonsRecyclerView.visibility = if (lessons.isEmpty()) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }

                        binding.emptyStateText.visibility = if (lessons.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }

                    state.selectedLesson?.let { lesson ->
                        studentLessonAdapter.updateSubBabsForLesson(lesson.id, state.subBabs)
                    }
                }
                is LessonViewModel.StudentState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun getLesson() {
        val idSubject = arguments?.getString("idSubject")

        if (idSubject != null) {
            viewModel.getLessonsBySubject(idSubject)
        } else {
            showToast(getString(R.string.data_not_completed))
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
        // Cleanup resources
        studentLessonAdapter.cleanup()
        _binding = null
    }
} 