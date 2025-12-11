package com.adika.learnable.view.dashboard.teacher

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.TeacherLessonAdapter
import com.adika.learnable.databinding.FragmentTeacherDashboardBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.User
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.util.DeleteConfirmDialog
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.view.auth.LogoutDialogFragment
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.teacher.dialog.LessonDetailDialogFragment
import com.adika.learnable.view.dashboard.teacher.form.LessonFormBottomSheetDialogFragment
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherDashboardFragment : BaseFragment() {
    private var _binding: FragmentTeacherDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeacherDashboardViewModel by viewModels()
    private val lessonViewModel: LessonViewModel by viewModels()

    private lateinit var lessonAdapter: TeacherLessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRecyclerView()
        setupDialogListener()
        observeViewModel()
        loadData()
        setupSearch()
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            showLoading(true)
            findNavController().navigate(R.id.action_teacherDashboardFragment_to_editProfileFragment)
        }

        binding.btnChangePassword.setOnClickListener {
            showLoading(true)
            findNavController().navigate(R.id.action_teacherDashboardFragment_to_changePasswordFragment)
        }

        binding.fabAddLesson.setOnClickListener {
            showLessonDialog()
        }

        binding.ivLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnLevel.setOnClickListener {
            showSchoolLevelFilterDialog()
        }

        binding.btnSubject.setOnClickListener {
            showSubjectFilterDialog()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString().orEmpty().trim()
            viewModel.searchLessons(query)
        }
    }

    private fun setupRecyclerView() {
        lessonAdapter = TeacherLessonAdapter(
            onViewClick = { lesson ->

                val action = TeacherDashboardFragmentDirections
                    .actionTeacherDashboardFragmentToTeacherSubbabListFragment(
                        idLesson = lesson.id,
                        lessonTitle = lesson.title
                    )
                findNavController().navigate(action)
            },
            onDetailClick = { lesson ->
                showLessonDetailDialog(lesson)
            },
            onEditClick = { lesson ->
                showLessonDialog(lesson)
            },
            onDeleteClick = { lesson ->
                confirmDeleteLesson(lesson)
            }
        )

        binding.rvLessons.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lessonAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.teacherState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        lessonViewModel.teacherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LessonViewModel.TeacherState.Success -> {
                    showLoading(false)
                    viewModel.setAllLessons(state.lessons)
                }

                is LessonViewModel.TeacherState.Loading -> {
                    showLoading(true)
                }

                is LessonViewModel.TeacherState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.filteredLessons.observe(viewLifecycleOwner) { lessons ->
            lessonAdapter.submitList(lessons)
            updateEmptyState(lessons.isEmpty())
        }

        viewModel.selectedSchoolLevel.observe(viewLifecycleOwner) { level ->
            updateSchoolLevelButtonText(level)
        }

        viewModel.selectedSubjectId.observe(viewLifecycleOwner) { subjectId ->
            updateSubjectButtonText(subjectId)
        }

        viewModel.subjectsForLevel.observe(viewLifecycleOwner) {

            updateSubjectButtonText(viewModel.selectedSubjectId.value)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is LessonViewModel.TeacherState.Loading,
            is TeacherDashboardViewModel.TeacherState.Loading,
            is TeacherDashboardViewModel.StudentState.Loading -> {
                showLoading(true)
            }

            is TeacherDashboardViewModel.TeacherState.Success -> {
                showLoading(false)
                state.teacher?.let { updateParentUI(it) }
                if (state.teacher != null) {
                    lessonViewModel.getLessonsByTeacherId(state.teacher.id)
                }
            }


            is LessonViewModel.TeacherState.Error,
            is TeacherDashboardViewModel.TeacherState.Error,
            is TeacherDashboardViewModel.StudentState.Error -> {
                showLoading(false)
                showToast(
                    (state as? LessonViewModel.TeacherState.Error)?.message
                        ?: (state as? TeacherDashboardViewModel.TeacherState.Error)?.message
                        ?: (state as? TeacherDashboardViewModel.StudentState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    private fun loadData() {
        viewModel.loadUserData()
    }

    private fun showLoading(isLoading: Boolean) {

        binding.btnEditProfile.isEnabled = !isLoading
        binding.btnChangePassword.isEnabled = !isLoading
        binding.etSearch.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateParentUI(parent: User) {
        binding.apply {
            tvName.text = parent.name
            tvEmail.text = parent.email

            parent.profilePicture.let { url ->
                Glide.with(requireContext())
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(profileImage)
            }
        }
    }

    private fun showLessonDetailDialog(lesson: Lesson) {
        val dialog = LessonDetailDialogFragment.newInstance(
            lesson = lesson,
            onViewSubbabClick = { lessons ->

                val action = TeacherDashboardFragmentDirections
                    .actionTeacherDashboardFragmentToTeacherSubbabListFragment(
                        idLesson = lessons.id,
                        lessonTitle = lessons.title
                    )
                findNavController().navigate(action)
            }
        )
        dialog.show(parentFragmentManager, "LessonDetailDialogFragment")
    }

    private fun showLessonDialog(lesson: Lesson? = null) {
        val dialog = LessonFormBottomSheetDialogFragment.newInstance(lesson) { newLesson ->
            if (lesson == null) {
                lessonViewModel.addLesson(newLesson) { result ->
                    result.onSuccess {

                        // The observer will automatically update the UI
                        showToast(getString(R.string.succes_add_lesson))
                    }.onFailure {
                        showToast(it.message ?: getString(R.string.unknown_error))
                    }
                }
            } else {
                lessonViewModel.updateLesson(newLesson) { res ->
                    res.onSuccess {

                        // The observer will automatically update the UI
                        showToast(getString(R.string.succes_update_lesson))
                    }.onFailure {
                        showToast(it.message ?: getString(R.string.unknown_error))
                    }
                }
            }
        }
        dialog.show(parentFragmentManager, "LessonFormBottomSheetDialogFragment")
    }

    private fun confirmDeleteLesson(lesson: Lesson) {
        val teacherId =
            (viewModel.teacherState.value as? TeacherDashboardViewModel.TeacherState.Success)?.teacher?.id

        DeleteConfirmDialog.show(
            fragment = this,
            titleRes = R.string.title_delete,
            messageRes = R.string.subtitle_delete_lesson,
            onConfirm = {
                if (teacherId != null) {
                    lessonViewModel.deleteLesson(lesson.id, teacherId) { result ->
                        result.onFailure {
                            showToast(
                                it.message ?: getString(R.string.unknown_error)
                            )
                        }
                    }
                }
            }
        )
    }

    private fun setupDialogListener() {
        childFragmentManager.setFragmentResultListener(
            LogoutDialogFragment.REQ, viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(LogoutDialogFragment.ACTION)) {
                LogoutDialogFragment.ACTION_BACK_TO_LOGIN -> {
                    viewModel.logout()
                    findNavController().navigate(R.id.action_teacherDashboardFragment_to_login)
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        LogoutDialogFragment().show(childFragmentManager, LogoutDialogFragment.TAG)
    }

    private fun showSchoolLevelFilterDialog() {
        val schoolLevels = resources.getStringArray(R.array.school_levels)
        val initials = schoolLevels.map { getInitial(it) }
        val levelOptions = listOf(getString(R.string.all_level)) + initials.toList()
        val currentLevel = viewModel.selectedSchoolLevel.value
        val currentIndex = when (currentLevel) {
            null -> 0
            EducationLevels.SD -> 1
            EducationLevels.SMP -> 2
            EducationLevels.SMA -> 3
            else -> 0
        }

        val popup = ListPopupWindow(requireContext())
        val adapter = object : ArrayAdapter<String>(requireContext(), 0, levelOptions) {
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
        popup.anchorView = binding.btnLevel
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_dropdown_panel)
        )

        binding.btnLevel.post {
            val paint = binding.btnLevel.paint
            var maxWidth = 0
            for (option in levelOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }
            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding
            if (popup.width < binding.btnLevel.width) {
                popup.width = binding.btnLevel.width
            }
            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val level = when (position) {
                0 -> null
                1 -> EducationLevels.SD
                2 -> EducationLevels.SMP
                3 -> EducationLevels.SMA
                else -> null
            }
            viewModel.applyClassFilter(level)
            popup.dismiss()
        }
    }

    private fun showSubjectFilterDialog() {
        val currentLevel = viewModel.selectedSchoolLevel.value
        if (currentLevel == null) {
            Toast.makeText(requireContext(), "Pilih jenjang terlebih dahulu", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val subjectsForLevel = viewModel.subjectsForLevel.value ?: emptyList()
        if (subjectsForLevel.isEmpty()) {
            Toast.makeText(requireContext(), "Memuat pelajaran...", Toast.LENGTH_SHORT).show()
            return
        }

        val subjectOptions =
            listOf(getString(R.string.all_subject)) + subjectsForLevel.map { it.name }
        val selectedSubjectId = viewModel.selectedSubjectId.value

        val currentIndex = if (selectedSubjectId == null) {
            0
        } else {
            val foundSubject = subjectsForLevel.find { it.idSubject == selectedSubjectId }
            if (foundSubject != null) {
                subjectOptions.indexOf(foundSubject.name).takeIf { it >= 0 } ?: 0
            } else {
                0
            }
        }

        val popup = ListPopupWindow(requireContext())
        val adapter = object : ArrayAdapter<String>(requireContext(), 0, subjectOptions) {
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
        popup.anchorView = binding.btnSubject
        popup.isModal = true
        popup.setBackgroundDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_dropdown_panel)
        )

        binding.btnSubject.post {
            val paint = binding.btnSubject.paint
            var maxWidth = 0
            for (option in subjectOptions) {
                val textWidth = paint.measureText(option).toInt()
                if (textWidth > maxWidth) {
                    maxWidth = textWidth
                }
            }
            val iconWidth = 40
            val horizontalPadding = 48
            val extraPadding = 24
            popup.width = maxWidth + iconWidth + horizontalPadding + extraPadding
            if (popup.width < binding.btnSubject.width) {
                popup.width = binding.btnSubject.width
            }
            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val subjectId = if (position == 0) {
                null
            } else {
                subjectsForLevel[position - 1].idSubject
            }
            viewModel.setSelectedSubject(subjectId)
            popup.dismiss()
        }
    }

    private fun updateSchoolLevelButtonText(level: String?) {
        val text = when (level) {
            null -> getString(R.string.all_level)
            EducationLevels.SD -> resources.getStringArray(R.array.school_levels)[0]
            EducationLevels.SMP -> resources.getStringArray(R.array.school_levels)[1]
            EducationLevels.SMA -> resources.getStringArray(R.array.school_levels)[2]
            else -> getString(R.string.all_level)
        }
        binding.btnLevel.text = text
    }

    private fun updateSubjectButtonText(subjectId: String?) {
        val subjectsForLevel = viewModel.subjectsForLevel.value ?: emptyList()
        val text = if (subjectId == null) {
            getString(R.string.all_subject)
        } else {
            val subject = subjectsForLevel.find { it.idSubject == subjectId }
            subject?.name ?: getString(R.string.all_subject)
        }
        binding.btnSubject.text = text
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyLayout.visibility = View.VISIBLE
                rvLessons.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                rvLessons.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}