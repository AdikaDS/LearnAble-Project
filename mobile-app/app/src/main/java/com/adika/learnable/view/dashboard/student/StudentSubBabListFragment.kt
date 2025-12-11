package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.util.Log
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
import com.adika.learnable.adapter.StudentSubBabAdapter
import com.adika.learnable.databinding.FragmentSubBabListBinding
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.util.BookmarkDialogUtils
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@AndroidEntryPoint
class StudentSubBabListFragment : BaseFragment() {
    private var _binding: FragmentSubBabListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LessonViewModel by viewModels()
    private val args: StudentSubBabListFragmentArgs by navArgs()
    private lateinit var subBabAdapter: StudentSubBabAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubBabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
        getSubBab()
        setupOnClickListener()
        setImageHeader()

        setupTextScaling()
    }

    private fun setupRecyclerView() {
        subBabAdapter = StudentSubBabAdapter(
            onSubBabClick = { subBab ->
                val lessonName = args.lessonName ?: ""
                val subjectName = args.subjectName ?: ""
                val subjectId = args.idSubject

                val action = StudentSubBabListFragmentDirections
                    .actionStudentSubBabListFragmentToStudentSubBabDetailFragment(
                        subBab = subBab,
                        subjectName = subjectName,
                        lessonName = lessonName,
                        subjectId = subjectId
                    )
                findNavController().navigate(action)
            },
            onBookmarkClick = { subBab ->
                val idLesson = args.idLesson ?: return@StudentSubBabAdapter
                val subjectId = args.idSubject

                viewModel.checkBookmarkStatusForSubBabs(
                    idLesson,
                    listOf(subBab)
                ) { _, isCurrentlyBookmarked ->

                    if (isCurrentlyBookmarked) {

                        BookmarkDialogUtils.showBookmarkDeleteConfirmDialog(
                            fragment = this@StudentSubBabListFragment,
                            onConfirm = {

                                viewModel.toggleBookmarkForCurrentUser(
                                    idLesson,
                                    subBab,
                                    subjectId
                                ) { isBookmarked ->
                                    subBabAdapter.updateBookmarkStatus(subBab.id, isBookmarked)
                                }
                            },
                            onCancel = {

                            }
                        )
                    } else {

                        Log.d("StudentSubBabListFragment", "Adding bookmark")
                        viewModel.toggleBookmarkForCurrentUser(
                            idLesson,
                            subBab,
                            subjectId
                        ) { isBookmarked ->
                            Log.d("StudentSubBabListFragment", "Bookmark toggled to: $isBookmarked")
                            subBabAdapter.updateBookmarkStatus(subBab.id, isBookmarked)
                            if (isBookmarked) {

                                BookmarkDialogUtils.showBookmarkSuccessDialog(
                                    fragment = this@StudentSubBabListFragment
                                )
                            }
                        }
                    }
                }
            }
        )

        binding.subbabRecyclerView.apply {
            adapter = subBabAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val idLesson = arguments?.getString("idLesson") ?: return@addTextChangedListener
            viewModel.searchSubBabs(
                query = text?.toString() ?: "",
                idLesson = idLesson
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

        viewModel.subBabProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.subBabCount.observe(viewLifecycleOwner) { count ->
            binding.tvChapterCount.text = getString(R.string.total_subbab, count.toString())
        }

        viewModel.lesson.observe(viewLifecycleOwner) { lesson ->
            lesson?.coverImage?.let { coverImageUrl ->
                Glide.with(requireContext())
                    .load(coverImageUrl)
                    .placeholder(R.drawable.icon_dummy_bab)
                    .error(R.drawable.icon_dummy_bab)
                    .into(binding.ivHeader)
            }
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is LessonViewModel.StudentState.Loading,
            is LessonViewModel.LessonProgressState.Loading,
            is LessonViewModel.SubBabProgressState.Loading -> {
                showLoading(true)
            }

            is LessonViewModel.StudentState.Success -> {
                showLoading(false)
                state.subBabs.let { subbab ->

                    if (subbab.isNotEmpty()) {
                        viewModel.loadSubBabProgress(subbab.map { it.id })

                        checkBookmarkStatusForSubBabs(subbab)
                    }

                    subBabAdapter.submitList(subbab)

                    binding.subbabRecyclerView.visibility = if (subbab.isEmpty()) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }

                    binding.emptyStateText.visibility = if (subbab.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    setLessonTitleFromArgs()
                }
            }

            is LessonViewModel.SubBabProgressState.Success -> {
                showLoading(false)
                val progress = state.progress
                if (progress != null) {
                    subBabAdapter.updateProgress(
                        progress.subBabId,
                        progress
                    )
                }
            }

            is LessonViewModel.LessonProgressState.Success -> {
                showLoading(false)
                updateProgressLesson(state.progress)
            }

            is LessonViewModel.StudentState.Error,
            is LessonViewModel.LessonProgressState.Error,
            is LessonViewModel.SubBabProgressState.Error -> {
                showLoading(false)
                val errorMessage = (state as? LessonViewModel.StudentState.Error)?.message
                    ?: (state as? LessonViewModel.LessonProgressState.Error)?.message
                    ?: (state as? LessonViewModel.SubBabProgressState.Error)?.message
                    ?: getString(R.string.unknown_error)
                showToast(errorMessage)
            }
        }
    }

    private fun getSubBab() {
        val idSubBab = arguments?.getString("idLesson")

        if (idSubBab != null) {
            viewModel.getSubBabsByLesson(idSubBab)
            viewModel.loadSubBabCountByLesson(idSubBab)

            viewModel.syncTotalSubBabForLesson(idSubBab)
        } else {
            showToast(getString(R.string.data_not_completed))
        }
    }

    private fun setImageHeader() {
        val idLesson = args.idLesson

        if (idLesson != null) {
            viewModel.getLesson(idLesson)
        }

        binding.ivHeader.post {
            val headerHeight = binding.ivHeader.height
            val overlayHeight = (headerHeight * 0.9).toInt()

            val layoutParams = binding.headerOverlay.layoutParams
            layoutParams.height = overlayHeight
            binding.headerOverlay.layoutParams = layoutParams
        }
    }

    private fun setLessonTitleFromArgs() {
        val lessonName = arguments?.getString("lessonName")
        if (!lessonName.isNullOrEmpty()) {
            binding.tvSubBabTitle.text = lessonName
        }
    }

    private fun updateProgressLesson(progress: StudentLessonProgress?) {
        val completed = progress?.completedSubBabs ?: 0
        val totalFromProgress = progress?.totalSubBabs ?: 0
        val percentage: Int = if (totalFromProgress > 0) {
            val raw = ((completed.toFloat() * 100f) / totalFromProgress.toFloat()).roundToInt()
            min(100, max(0, raw))
        } else 0
        binding.headerProgressBar.progress = percentage

        binding.tvHeaderProgress.text =
            getString(R.string.progress_text, completed, totalFromProgress)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun checkBookmarkStatusForSubBabs(subBabs: List<com.adika.learnable.model.SubBab>) {
        val idLesson = args.idLesson ?: return
        viewModel.checkBookmarkStatusForSubBabs(idLesson, subBabs) { subBabId, isBookmarked ->
            subBabAdapter.updateBookmarkStatus(subBabId, isBookmarked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        subBabAdapter.cleanup()
    }
}