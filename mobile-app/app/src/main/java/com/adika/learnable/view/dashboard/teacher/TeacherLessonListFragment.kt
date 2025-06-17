package com.adika.learnable.view.dashboard.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.TeacherLessonAdapter
import com.adika.learnable.databinding.FragmentTeacherLessonListBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.SubBab
import com.adika.learnable.viewmodel.LessonViewModel
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherLessonListFragment : Fragment() {

    private var _binding: FragmentTeacherLessonListBinding? = null
    private val binding get() = _binding!!

    private val lessonViewModel: LessonViewModel by viewModels()
    private val teacherDashboardViewModel: TeacherDashboardViewModel by viewModels()

    private lateinit var lessonAdapter: TeacherLessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherLessonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        getData()

    }

    private fun setupRecyclerView() {
        lessonAdapter = TeacherLessonAdapter(
            onLessonClick = { lesson ->
                if (lessonAdapter.isLessonExpanded(lesson.id)) {
                    lessonViewModel.loadSubBabsForLessonTeacher(lesson.id)
                    binding.fabAddSubBab.visibility = View.VISIBLE
                } else {
                    binding.fabAddSubBab.visibility = View.GONE
                }
            },
            onEditClick = { lesson ->
                showLessonDialog(lesson)
            },
            onDeleteClick = { lesson ->
                showDeleteLessonConfirmation(lesson)
            },
            onSubBabEditClick = { subBab ->
                showSubBabDialog(subBab = subBab)
            },
            onSubBabDeleteClick = { subBab ->
                showDeleteSubBabConfirmation(subBab)
            }
        )

        binding.rvLessons.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lessonAdapter
        }
    }

    private fun setupObservers() {
        lessonViewModel.teacherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LessonViewModel.TeacherState.Loading -> {
                    showLoading(true)
                }

                is LessonViewModel.TeacherState.Success -> {
                    showLoading(false)
                    lessonAdapter.submitList(state.lessons)
                    if (state.selectedLesson != null) {
                        lessonAdapter.updateSubBabsForLesson(state.selectedLesson.id, state.subBabs)
                    }
                }

                is LessonViewModel.TeacherState.Error -> {
                    showLoading(false)
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        teacherDashboardViewModel.teacherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TeacherDashboardViewModel.TeacherState.Loading -> {
                    showLoading(true)
                }

                is TeacherDashboardViewModel.TeacherState.Success -> {
                    // Load lessons using the teacher's ID
                    lessonViewModel.getLessonsByTeacherId(state.teacher.id)
                }

                is TeacherDashboardViewModel.TeacherState.Error -> {
                    showToast(state.message)
                }
            }
        }
    }


    private fun setupClickListeners() {
        binding.fabAddLesson.setOnClickListener {
            showLessonDialog()
        }

        binding.fabAddSubBab.setOnClickListener {
            val currentState = lessonViewModel.teacherState.value
            if (currentState is LessonViewModel.TeacherState.Success) {
                currentState.selectedLesson?.let { selectedLesson ->
                    showSubBabDialog(lessonId = selectedLesson.id)
                }
            }
        }
    }

    private fun getData() {
        teacherDashboardViewModel.loadUserData()
    }

    private fun showLessonDialog(lesson: Lesson? = null) {
        LessonFormBottomSheetDialogFragment.newInstance(lesson) { newLesson ->
            if (lesson == null) {
                lessonViewModel.addLesson(newLesson) { result ->
                    result.onSuccess {
                        showToast(getString(R.string.succes_add_lesson))
                    }.onFailure {
                        showToast(getString(R.string.fail_add_lesson, it.message))
                    }
                }
            } else {
                lessonViewModel.updateLesson(newLesson) { result ->
                    result.onSuccess {
                        showToast(getString(R.string.succes_update_lesson))
                    }.onFailure {
                        showToast(getString(R.string.fail_update_lesson, it.message))
                    }
                }
            }
        }.show(childFragmentManager, "LessonFormDialog")
    }

    private fun showSubBabDialog(subBab: SubBab? = null, lessonId: String? = null) {
        SubBabFormBottomSheetDialogFragment.newInstance(subBab, lessonId) { newSubBab ->
            if (subBab == null) {
                lessonViewModel.addSubBab(newSubBab) { result ->
                    result.onSuccess {
                        showToast(getString(R.string.succes_add_subbab))
                    }.onFailure {
                        showToast(getString(R.string.fail_add_subbab, it.message))
                    }
                }
            } else {
                lessonViewModel.updateSubBab(newSubBab) { result ->
                    result.onSuccess {
                        showToast(getString(R.string.succes_update_subbab))
                    }.onFailure {
                        showToast(getString(R.string.fail_update_subbab, it.message))
                    }
                }
            }
        }.show(childFragmentManager, "SubBabFormDialog")
    }

    private fun showDeleteLessonConfirmation(lesson: Lesson) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_lesson))
            .setMessage(getString(R.string.confirm_delete_lesson))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lessonViewModel.deleteLesson(lesson.id, lesson.teacherId) { result ->
                    result.onSuccess {
                       showToast(getString(R.string.succes_delete_lesson))
                    }.onFailure {
                        showToast(getString(R.string.fail_delete_lesson, it.message))
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteSubBabConfirmation(subBab: SubBab) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_subbab))
            .setMessage(getString(R.string.confirm_delete_subbab))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lessonViewModel.deleteSubBab(subBab.id, subBab.lessonId) { result ->
                    result.onSuccess {
                        showToast(getString(R.string.succes_delete_subbab))
                    }.onFailure {
                        showToast(getString(R.string.fail_delete_subbab, it.message))
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
