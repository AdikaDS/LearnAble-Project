package com.adika.learnable.view.dashboard.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.TeacherSubbabListAdapter
import com.adika.learnable.databinding.FragmentTeacherSubbabListBinding
import com.adika.learnable.model.SubBab
import com.adika.learnable.util.DeleteConfirmDialog
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.teacher.dialog.SubbabDetailDialogFragment
import com.adika.learnable.view.dashboard.teacher.form.SubBabFormBottomSheetDialogFragment
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import com.adika.learnable.viewmodel.lesson.MaterialViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeacherSubbabListFragment : BaseFragment() {
    private var _binding: FragmentTeacherSubbabListBinding? = null
    private val binding get() = _binding!!
    private val args: TeacherSubbabListFragmentArgs by navArgs()
    private val lessonViewModel: LessonViewModel by viewModels()
    private val teacherDashboardViewModel: TeacherDashboardViewModel by viewModels()
    private val materialViewModel: MaterialViewModel by viewModels()

    private lateinit var subbabAdapter: TeacherSubbabListAdapter
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherSubbabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
        setupLessonTitle()
        loadData()
        setupTextScaling()
    }

    private fun setupRecyclerView() {
        subbabAdapter = TeacherSubbabListAdapter(
            onDetailClick = { subBab ->
                showSubbabDetailDialog(subBab)
            },
            onEditClick = { subBab ->
                showSubBabDialog(subBab = subBab)
            },
            onDeleteClick = { subBab ->
                showDeleteSubBabConfirmation(subBab)
            }
        )

        binding.rvSubbabList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subbabAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddSubbab.setOnClickListener {
            showSubBabDialog(lessonId = args.idLesson)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString().orEmpty().trim()
            searchSubBabs(query)
        }
    }

    private fun observeViewModel() {
        lessonViewModel.teacherState.observe(viewLifecycleOwner) { state ->
            handleLessonState(state)
        }

        teacherDashboardViewModel.teacherState.observe(viewLifecycleOwner) { state ->
            handleTeacherState(state)
        }
    }

    private fun handleLessonState(state: LessonViewModel.TeacherState) {
        when (state) {
            is LessonViewModel.TeacherState.Loading -> showLoading(true)

            is LessonViewModel.TeacherState.Success -> {
                showLoading(false)
                val filteredSubbabs = getFilteredSubbabs(state)
                val searchFilteredSubbabs = applySearchFilter(filteredSubbabs)
                updateSubbabsList(searchFilteredSubbabs)
            }

            is LessonViewModel.TeacherState.Error -> {
                showLoading(false)
                showToast(state.message)
            }
        }
    }

    private fun handleTeacherState(state: TeacherDashboardViewModel.TeacherState) {
        if (state is TeacherDashboardViewModel.TeacherState.Success) {
            state.teacher?.let { teacher ->
                val currentLessonState = lessonViewModel.teacherState.value
                if (currentLessonState !is LessonViewModel.TeacherState.Success) {
                    lessonViewModel.getLessonsByTeacherId(teacher.id)
                }
            }
        }
    }

    private fun getFilteredSubbabs(state: LessonViewModel.TeacherState.Success): List<SubBab> {
        val idLesson = args.idLesson
        return when {
            state.selectedLesson?.id == idLesson -> state.subBabs
            state.subBabs.isNotEmpty() && state.subBabs.firstOrNull()?.lessonId == idLesson -> {
                state.subBabs.filter { it.lessonId == idLesson }
            }

            state.lessons.any { it.id == idLesson } -> {
                lessonViewModel.loadSubBabsForLessonTeacher(idLesson)
                emptyList()
            }

            else -> emptyList()
        }
    }

    private fun updateSubbabsList(subbabs: List<SubBab>) {
        subbabAdapter.submitList(subbabs)
        updateEmptyState(subbabs.isEmpty())
        binding.apply {
            rvSubbabList.visibility = if (subbabs.isEmpty()) View.GONE else View.VISIBLE
            emptyLayout.visibility = if (subbabs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupLessonTitle() {
        val lessonTitle = args.lessonTitle
        if (!lessonTitle.isNullOrEmpty()) {
            binding.tvSectionTitle.text = lessonTitle
        }
    }

    private fun loadData() {
        val idLesson = args.idLesson
        if (idLesson.isEmpty()) {
            showToast(getString(R.string.data_not_completed))
            return
        }

        val currentState = lessonViewModel.teacherState.value
        when (currentState) {
            is LessonViewModel.TeacherState.Success -> {

                val lessonExists = currentState.lessons.any { it.id == idLesson }
                if (lessonExists) {

                    lessonViewModel.loadSubBabsForLessonTeacher(idLesson)
                } else {

                    // This will be handled by observeViewModel when teacher data is loaded
                    teacherDashboardViewModel.loadUserData()
                }
            }

            null, is LessonViewModel.TeacherState.Error -> {

                teacherDashboardViewModel.loadUserData()
            }

            is LessonViewModel.TeacherState.Loading -> {

            }
        }
    }

    private fun searchSubBabs(query: String) {
        currentSearchQuery = query
        val idLesson = args.idLesson
        if (idLesson.isEmpty()) return

        val currentState = lessonViewModel.teacherState.value
        if (currentState is LessonViewModel.TeacherState.Success) {
            val filteredSubbabs = getFilteredSubbabs(currentState)
            val searchFilteredSubbabs = applySearchFilter(filteredSubbabs)
            updateSubbabsList(searchFilteredSubbabs)
        } else {

            lessonViewModel.loadSubBabsForLessonTeacher(idLesson)
        }
    }

    private fun applySearchFilter(subbabs: List<SubBab>): List<SubBab> {
        if (currentSearchQuery.isBlank()) {
            return subbabs
        }

        val query = currentSearchQuery.lowercase().trim()
        return subbabs.filter { subBab ->
            subBab.title.lowercase().contains(query)
        }
    }

    private fun showSubbabDetailDialog(subBab: SubBab) {
        val lessonTitle = args.lessonTitle
        val dialog = SubbabDetailDialogFragment.newInstance(
            subBab = subBab,
            lessonTitle = lessonTitle,
            onViewMateriClick = { pdfUrl ->
                openPdfViewer(pdfUrl)
            },
            onViewVideoClick = { videoUrl ->
                openVideoPlayer(videoUrl, subBab.subtitle)
            },
            onViewQuizClick = { subBabs ->
                openQuizDetail(subBabs.id)
            }
        )
        dialog.show(parentFragmentManager, "SubbabDetailDialogFragment")
    }

    private fun openPdfViewer(pdfUrl: String) {
        try {
            val action = TeacherSubbabListFragmentDirections
                .actionTeacherSubbabListFragmentToPdfViewerFragment(
                    pdfUrl = pdfUrl
                )
            findNavController().navigate(action)
        } catch (e: Exception) {
            showToast("Gagal membuka PDF viewer: ${e.message}")
        }
    }

    private fun openVideoPlayer(videoUrl: String, subtitleUrl: String? = null) {
        try {
            val subtitle = subtitleUrl.takeIf { !it.isNullOrBlank() }
            val action = TeacherSubbabListFragmentDirections
                .actionTeacherSubbabListFragmentToVideoPlayerFragment(
                    videoUrl = videoUrl,
                    subtitleUrl = subtitle
                )
            findNavController().navigate(action)
        } catch (e: Exception) {
            showToast("Gagal membuka video player: ${e.message}")
        }
    }

    private fun openQuizDetail(subBabId: String) {
        try {
            val action = TeacherSubbabListFragmentDirections
                .actionTeacherSubbabListFragmentToQuizDetailFragment(
                    subBabId = subBabId
                )
            findNavController().navigate(action)
        } catch (e: Exception) {
            showToast("Gagal membuka detail kuis: ${e.message}")
        }
    }

    private fun showSubBabDialog(subBab: SubBab? = null, lessonId: String? = null) {
        val idLesson = lessonId ?: args.idLesson
        SubBabFormBottomSheetDialogFragment.newInstance(
            subBab = subBab,
            lessonId = idLesson,
            onSave = { _ ->

                reloadSubBabsData(idLesson)
                lessonViewModel.syncTotalSubBabForLesson(idLesson)
            }
        ).show(childFragmentManager, "SubBabFormDialog")
    }

    private fun reloadSubBabsData(idLesson: String) {
        val currentState = lessonViewModel.teacherState.value
        if (currentState is LessonViewModel.TeacherState.Success) {
            lessonViewModel.loadSubBabsForLessonTeacher(idLesson)
        } else {
            binding.root.postDelayed({
                val newState = lessonViewModel.teacherState.value
                if (newState is LessonViewModel.TeacherState.Success) {
                    lessonViewModel.loadSubBabsForLessonTeacher(idLesson)
                }
            }, 300)
        }
    }

    private fun showDeleteSubBabConfirmation(subBab: SubBab) {
        DeleteConfirmDialog.show(
            fragment = this,
            titleRes = R.string.title_delete,
            messageRes = R.string.confirm_delete_subbab,
            onConfirm = {

                materialViewModel.deleteAllMaterialsForSubBab(subBab.id) { delRes ->
                    delRes.fold(
                        onSuccess = {
                            lessonViewModel.deleteSubBab(subBab.id, subBab.lessonId) { result ->
                                result.onSuccess {
                                    reloadSubBabsData(args.idLesson)
                                    lessonViewModel.syncTotalSubBabForLesson(args.idLesson)
                                }.onFailure {
                                    showToast(it.message ?: getString(R.string.unknown_error))
                                }
                            }
                        },
                        onFailure = {
                            showToast(it.message ?: getString(R.string.unknown_error))
                        }
                    )
                }
            }
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyLayout.visibility = View.VISIBLE
                rvSubbabList.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                rvSubbabList.visibility = View.VISIBLE
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {

    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
