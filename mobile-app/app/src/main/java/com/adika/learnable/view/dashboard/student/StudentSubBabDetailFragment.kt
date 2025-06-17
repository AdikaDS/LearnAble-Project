package com.adika.learnable.view.dashboard.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentStudentSubBabDetailBinding
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.view.dashboard.student.material.AudioPlayerFragment
import com.adika.learnable.view.dashboard.student.material.PdfViewerFragment
import com.adika.learnable.view.dashboard.student.material.VideoPlayerFragment
import com.adika.learnable.viewmodel.StudentSubBabDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class StudentSubBabDetailFragment : Fragment() {
    private var _binding: FragmentStudentSubBabDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentSubBabDetailViewModel by viewModels()
    private val args: StudentSubBabDetailFragmentArgs by navArgs()
    private var startTime: Long = 0

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
        updateUI(args.subBab)
        setupMediaButtons()
        setupQuizButton()
        observeViewModel()
        viewModel.getSubBab(args.subBab.id)
        startTime = System.currentTimeMillis()
    }

    private fun setupMediaButtons() {
        // Set up media button click listeners
        binding.videoButton.setOnClickListener {
            args.subBab.mediaUrls["video"]?.let { url ->
                showVideoPlayer(url)
            }
        }

        binding.audioButton.setOnClickListener {
            args.subBab.mediaUrls["audio"]?.let { url ->
                showAudioPlayer(url)
            }
        }

        binding.pdfButton.setOnClickListener {
            args.subBab.mediaUrls["pdfLesson"]?.let { url ->
                showPdfViewer(url)
            }
        }
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
                        updateProgressUI(progress)
                    }
                }

                is StudentSubBabDetailViewModel.ProgressState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun updateUI(subBab: SubBab) {
        binding.apply {
            titleText.text = subBab.title
            contentText.text = subBab.content
            durationText.text = getString(R.string.duration_learning, subBab.duration)

            // Enable/disable media buttons based on URL availability
            videoButton.isEnabled = !subBab.mediaUrls["video"].isNullOrEmpty()
            audioButton.isEnabled = !subBab.mediaUrls["audio"].isNullOrEmpty()
            pdfButton.isEnabled = !subBab.mediaUrls["pdfLesson"].isNullOrEmpty()
        }
    }

    private fun updateProgressUI(progress: StudentSubBabProgress) {
        binding.apply {
            // Update button states based on material availability
            videoButton.isEnabled = !args.subBab.mediaUrls["video"].isNullOrEmpty()
            audioButton.isEnabled = !args.subBab.mediaUrls["audio"].isNullOrEmpty()
            pdfButton.isEnabled = !args.subBab.mediaUrls["pdfLesson"].isNullOrEmpty()
        }
    }

    private fun showVideoPlayer(url: String) {
        val videoFragment = VideoPlayerFragment.newInstance(url)
        videoFragment.setOnVideoCompletedListener {
            viewModel.markMaterialAsCompleted(args.subBab.id, "video")
        }
        childFragmentManager.beginTransaction()
            .replace(binding.mediaContainer.id, videoFragment)
            .commit()
    }

    private fun showAudioPlayer(url: String) {
        val audioFragment = AudioPlayerFragment.newInstance(url)
        audioFragment.setOnAudioCompletedListener {
            viewModel.markMaterialAsCompleted(args.subBab.id, "audio")
        }
        childFragmentManager.beginTransaction()
            .replace(binding.mediaContainer.id, audioFragment)
            .commit()
    }

    private fun showPdfViewer(url: String) {
        val pdfFragment = PdfViewerFragment.newInstance(url)
        pdfFragment.setOnPdfCompletedListener {
            viewModel.markMaterialAsCompleted(args.subBab.id, "pdf")
        }
        childFragmentManager.beginTransaction()
            .replace(binding.mediaContainer.id, pdfFragment)
            .commit()
    }

    private fun setupQuizButton() {
        binding.quizButton.setOnClickListener {
            val action =
                StudentSubBabDetailFragmentDirections.actionStudentSubBabDetailFragmentToQuizFragment(
                    args.subBab.id
                )
            findNavController().navigate(action)
        }
    }

    override fun onPause() {
        super.onPause()
        updateTimeSpent()
    }

    private fun updateTimeSpent() {
        val endTime = System.currentTimeMillis()
        val timeSpentMinutes = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime).toInt()
        if (timeSpentMinutes > 0) {
            viewModel.updateTimeSpent(args.subBab.id, timeSpentMinutes)
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