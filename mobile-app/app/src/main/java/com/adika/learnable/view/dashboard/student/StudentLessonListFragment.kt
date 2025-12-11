package com.adika.learnable.view.dashboard.student

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
import com.adika.learnable.adapter.StudentLessonAdapter
import com.adika.learnable.databinding.FragmentLessonListBinding
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.util.SubjectIconUtils.setSubjectIcon
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@AndroidEntryPoint
class StudentLessonListFragment : BaseFragment() {
    private var _binding: FragmentLessonListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()
    private val args: StudentLessonListFragmentArgs by navArgs()
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
        setupOnClickListener()
        setImageHeader()

        setupTextScaling()
    }

    private fun setupRecyclerView() {
        studentLessonAdapter = StudentLessonAdapter(
            onLessonClick = { lesson ->
                val action = StudentLessonListFragmentDirections
                    .actionStudentLessonListFragmentToStudentSubBabListFragment(
                        idSubject = args.idSubject,
                        idLesson = lesson.id,
                        lessonName = lesson.title,
                        subjectName = args.subjectName
                    )
                findNavController().navigate(action)
            }
        )

        binding.lessonsRecyclerView.apply {
            adapter = studentLessonAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchLessons(
                query = text?.toString() ?: "",
                idSubject = args.idSubject
            )
        }
    }

    private fun setupOnClickListener() {
        binding.apply {
            btnBack.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.lessonProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.subjectProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.lessonCount.observe(viewLifecycleOwner) { count ->
            binding.tvChapterCount.text = getString(R.string.total_bab, count.toString())
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is LessonViewModel.StudentState.Loading,
            is LessonViewModel.LessonProgressState.Loading,
            is LessonViewModel.SubjectProgressState.Loading -> {
                showLoading(true)
            }

            is LessonViewModel.StudentState.Success -> {
                showLoading(false)
                state.lessons?.let { lessons ->

                    if (lessons.isNotEmpty()) {
                        viewModel.loadLessonsProgress(lessons.map { it.id })
                    }

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

                    setTitleFromArgs()
                }
            }

            is LessonViewModel.LessonProgressState.Success -> {
                showLoading(false)
                val progress = state.progress
                if (progress != null) {
                    studentLessonAdapter.updateProgress(
                        progress.lessonId,
                        progress
                    )
                }
            }

            is LessonViewModel.SubjectProgressState.Success -> {
                showLoading(false)
                updateProgressSubject(state.progress)
            }

            is LessonViewModel.StudentState.Error,
            is LessonViewModel.LessonProgressState.Error,
            is LessonViewModel.SubjectProgressState.Error -> {
                showLoading(false)
                val errorMessage = (state as? LessonViewModel.StudentState.Error)?.message
                    ?: (state as? LessonViewModel.LessonProgressState.Error)?.message
                    ?: (state as? LessonViewModel.SubjectProgressState.Error)?.message
                    ?: getString(R.string.unknown_error)
                showToast(errorMessage)
            }
        }
    }

    private fun setImageHeader() {
        binding.ivHeader.setSubjectIcon(args.subjectName)
        binding.ivHeader.post {
            val headerHeight = binding.ivHeader.height
            val overlayHeight = (headerHeight * 1.1).toInt()

            val layoutParams = binding.headerOverlay.layoutParams
            layoutParams.height = overlayHeight
            binding.headerOverlay.layoutParams = layoutParams
        }


    }

    private fun getLesson() {
        val idSubject = args.idSubject

        viewModel.getLessonsBySubject(idSubject)
        viewModel.loadLessonCountBySubject(idSubject)
    }

    private fun setTitleFromArgs() {
        val subjectName = args.subjectName
        if (subjectName.isNotEmpty()) {
            binding.tvSubjectTitle.text = subjectName
        }

        val schoolLevel = args.schoolLevel
        val sd = getInitial(getString(R.string.elementarySchool))
        val smp = getInitial(getString(R.string.juniorHighSchool))
        val sma = getInitial(getString(R.string.seniorHighSchool))

        if (schoolLevel.isNotEmpty()) {
            val levelText = when (schoolLevel) {
                EducationLevels.SD -> sd
                EducationLevels.SMP -> smp
                EducationLevels.SMA -> sma
                else -> schoolLevel
            }

            binding.tvSchoolLevel.text = levelText
        }
    }

    private fun updateProgressSubject(progress: StudentSubjectProgress?) {
        val completed = progress?.completedLessons ?: 0
        val totalFromProgress = progress?.totalLessons ?: 0
        val percentage: Int = if (totalFromProgress > 0) {
            val raw = ((completed.toFloat() * 100f) / totalFromProgress.toFloat()).roundToInt()
            min(100, max(0, raw))
        } else 0
        binding.headerProgressBar.progress = percentage

        binding.tvHeaderProgress.text =
            getString(R.string.progress_text, completed, totalFromProgress)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        studentLessonAdapter.cleanup()
        _binding = null
    }
} 