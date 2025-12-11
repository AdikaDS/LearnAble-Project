package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.StepAdapter
import com.adika.learnable.databinding.FragmentStudentSubBabDetailBinding
import com.adika.learnable.model.StepItem
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.util.BookmarkDialogUtils
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.StudentSubBabDetailViewModel
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@UnstableApi
@AndroidEntryPoint
class StudentSubBabDetailFragment : BaseFragment() {
    private var _binding: FragmentStudentSubBabDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentSubBabDetailViewModel by viewModels()
    private val args: StudentSubBabDetailFragmentArgs by navArgs()
    private var startTime: Long = 0
    private lateinit var stepAdapter: StepAdapter
    private var shouldShowDialog = false
    private var isQuizCompleted = false

    private var timeTrackingHandler: Handler? = null
    private var timeTrackingRunnable: Runnable? = null
    private val TIME_UPDATE_INTERVAL_MS = 60_000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentSubBabDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupClickListeners()
        observeViewModel()
        viewModel.getSubBab(args.subBab.id)
        startTime = System.currentTimeMillis()

        setupTextScaling()
    }

    override fun onResume() {
        super.onResume()
        startTimeTracking()
    }

    override fun onPause() {
        super.onPause()
        stopTimeTracking()

        updateTimeSpent()
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StudentSubBabDetailViewModel.StudentState.Loading -> {
                    showLoading(true)
                }

                is StudentSubBabDetailViewModel.StudentState.Success -> {
                    showLoading(false)
                    updateUI(state.subBab)
                }

                is StudentSubBabDetailViewModel.StudentState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.progressState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StudentSubBabDetailViewModel.ProgressState.Loading -> {
                    showLoading(true)
                }

                is StudentSubBabDetailViewModel.ProgressState.Success -> {
                    showLoading(false)
                    state.progress?.let { progress ->
                        updateProgressUI(progress, state.latestQuizResult)
                    }
                }

                is StudentSubBabDetailViewModel.ProgressState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.bookmarkState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StudentSubBabDetailViewModel.BookmarkState.Loading -> {

                }

                is StudentSubBabDetailViewModel.BookmarkState.Success -> {
                    Log.d(
                        "StudentSubBabDetailFragment",
                        "Bookmark state success: ${state.isBookmarked}"
                    )
                    updateBookmarkButton(state.isBookmarked)

                    if (shouldShowDialog) {
                        shouldShowDialog = false // Reset flag

                        if (state.isBookmarked) {

                            Log.d(
                                "StudentSubBabDetailFragment",
                                "Showing delete confirmation dialog"
                            )
                            BookmarkDialogUtils.showBookmarkDeleteConfirmDialog(
                                fragment = this@StudentSubBabDetailFragment,
                                onConfirm = {
                                    Log.d("StudentSubBabDetailFragment", "User confirmed deletion")

                                    viewModel.toggleBookmark(args.subBab.id, args.subjectId)
                                },
                                onCancel = {
                                    Log.d("StudentSubBabDetailFragment", "User cancelled deletion")

                                }
                            )
                        } else {

                            Log.d("StudentSubBabDetailFragment", "Adding bookmark")
                            viewModel.toggleBookmark(args.subBab.id, args.subjectId)

                            BookmarkDialogUtils.showBookmarkSuccessDialog(
                                fragment = this@StudentSubBabDetailFragment
                            )
                        }
                    }
                }

                is StudentSubBabDetailViewModel.BookmarkState.Error -> {
                    showToast(state.message)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnBookmark.setOnClickListener {

            shouldShowDialog = true

            viewModel.checkBookmarkStatus(args.subBab.id)
        }
    }

    private fun updateUI(subBab: SubBab) {
        binding.apply {
            tvTitle.text = args.subjectName
            tvTitleLesson.text = args.lessonName
            tvTitleSubBab.text = subBab.title

            if (subBab.coverImage.isNotBlank()) {
                Glide.with(requireContext())
                    .load(subBab.coverImage)
                    .placeholder(R.drawable.icon_dummy_bab)
                    .error(R.drawable.icon_dummy_bab)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.icon_dummy_bab)
            }
        }

        val items = buildStepItems(subBab, null, null)
        stepAdapter.updateItems(items)

        viewModel.checkBookmarkStatus(subBab.id)
    }

    private fun updateProgressUI(
        progress: StudentSubBabProgress,
        latestQuizResult: com.adika.learnable.model.QuizResult?
    ) {
        isQuizCompleted = progress.completedMaterials["quiz"] == true

        val items = buildStepItems(args.subBab, progress, latestQuizResult)
        stepAdapter.updateItems(items)
    }

    private fun setupRecycler() {

        val videoUrls = mapOf(
            "video" to (args.subBab.mediaUrls["video"] ?: "")
        )

        stepAdapter = StepAdapter(
            items = mutableListOf(),
            onClick = { _, item ->
                when (item.key) {
                    "video" -> args.subBab.mediaUrls["video"]?.let { url ->
                        openVideoPlayer(url)
                        viewModel.markMaterialAsCompleted(args.subBab.id, "video")
                    }

                    "pdf" -> args.subBab.mediaUrls["pdfLesson"]?.let { url ->
                        openPdfViewer(url)
                        viewModel.markMaterialAsCompleted(args.subBab.id, "pdf")
                    }

                    "quiz" -> {
                        handleQuizNavigation()
                    }
                }
            },
            videoUrls = videoUrls
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stepAdapter
        }
    }

    private fun buildStepItems(
        subBab: SubBab,
        progress: StudentSubBabProgress?,
        latestQuizResult: com.adika.learnable.model.QuizResult? = null
    ): List<StepItem> {
        val completed = progress?.completedMaterials ?: emptyMap()
        val items = mutableListOf<StepItem>()

        if (!subBab.mediaUrls["video"].isNullOrEmpty()) {
            items.add(
                StepItem(
                    key = "video",
                    title = getString(R.string.video) + " - " + subBab.title,
                    isCompleted = completed["video"] == true
                )
            )
        }

        if (!subBab.mediaUrls["pdfLesson"].isNullOrEmpty()) {
            items.add(
                StepItem(
                    key = "pdf",
                    title = getString(R.string.material) + " - " + subBab.title,
                    isCompleted = completed["pdf"] == true
                )
            )
        }

        val quizScore = progress?.quizScore
        val quizScoreText = if (quizScore != null && quizScore > 0f) {

            val base = quizScore.toInt().toString()

            if (latestQuizResult?.essayGrading?.isEmpty() == true) {
                base + "\n" + getString(R.string.essay_not_graded)
            } else {
                base
            }
        } else {
            null
        }

        items.add(
            StepItem(
                key = "quiz",
                title = getString(R.string.quiz) + " - " + subBab.title,
                isCompleted = completed["quiz"] == true,
                scoreText = quizScoreText
            )
        )

        return items
    }

    private fun openVideoPlayer(videoUrl: String) {
        try {
            Log.d("StudentSubBabDetail", "=== OPEN VIDEO PLAYER DEBUG ===")
            Log.d("StudentSubBabDetail", "Video URL: $videoUrl")
            Log.d("StudentSubBabDetail", "SubBab subtitle field: '${args.subBab.subtitle}'")
            Log.d(
                "StudentSubBabDetail",
                "SubBab subtitle is empty: ${args.subBab.subtitle.isEmpty()}"
            )
            Log.d(
                "StudentSubBabDetail",
                "SubBab subtitle is blank: ${args.subBab.subtitle.isBlank()}"
            )

            val subtitleUrl = args.subBab.subtitle.takeIf { it.isNotEmpty() }
            Log.d("StudentSubBabDetail", "Subtitle URL to send: $subtitleUrl")

            val action =
                StudentSubBabDetailFragmentDirections.actionStudentSubBabDetailFragmentToVideoPlayerFragment(
                    videoUrl = videoUrl,
                    subtitleUrl = subtitleUrl, // Use subtitle from SubBab model
                )
            findNavController().navigate(action)

        } catch (e: Exception) {
            Log.e("StudentSubBabDetail", "Error opening video player", e)
            Toast.makeText(requireContext(), "Gagal membuka video player", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun openPdfViewer(pdfUrl: String) {
        try {

            val action =
                StudentSubBabDetailFragmentDirections.actionStudentSubBabDetailFragmentToPdfViewerFragment(
                    pdfUrl = pdfUrl,
                )
            findNavController().navigate(action)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka PDF viewer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimeTracking() {
        if (timeTrackingHandler == null) {
            timeTrackingHandler = Handler(Looper.getMainLooper())
        }

        timeTrackingRunnable = object : Runnable {
            override fun run() {

                viewModel.updateTimeSpent(args.subBab.id, 1)
                Log.d(
                    "StudentSubBabDetailFragment",
                    "Time tracking: Added 1 minute to subBab ${args.subBab.id}"
                )

                timeTrackingHandler?.postDelayed(this, TIME_UPDATE_INTERVAL_MS)
            }
        }

        timeTrackingHandler?.postDelayed(timeTrackingRunnable!!, TIME_UPDATE_INTERVAL_MS)
    }

    private fun stopTimeTracking() {
        timeTrackingRunnable?.let { runnable ->
            timeTrackingHandler?.removeCallbacks(runnable)
        }
        timeTrackingRunnable = null
    }

    private fun updateTimeSpent() {
        val endTime = System.currentTimeMillis()
        val timeSpentMinutes = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime).toInt()
        if (timeSpentMinutes > 0) {
            viewModel.updateTimeSpent(args.subBab.id, timeSpentMinutes)
            Log.d(
                "StudentSubBabDetailFragment",
                "Final time update: Added $timeSpentMinutes minutes to subBab ${args.subBab.id}"
            )
        }

        startTime = System.currentTimeMillis()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updateBookmarkButton(isBookmarked: Boolean) {
        binding.btnBookmark.setImageResource(
            if (isBookmarked) {
                R.drawable.ic_bookmark_filled
            } else {
                R.drawable.ic_bookmark
            }
        )
    }

    private fun handleQuizNavigation() {

        if (isQuizCompleted) {
            val action =
                StudentSubBabDetailFragmentDirections.actionStudentSubBabDetailFragmentToQuizResultFragment(
                    args.subBab
                )
            findNavController().navigate(action)
        } else {
            val action =
                StudentSubBabDetailFragmentDirections.actionStudentSubBabDetailFragmentToQuizFragment(
                    args.subBab
                )
            findNavController().navigate(action)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimeTracking()
        timeTrackingHandler = null
        _binding = null
    }
} 