package com.adika.learnable.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.adika.learnable.databinding.FragmentRecordingBinding
import com.adika.learnable.viewmodel.RecordingState
import com.adika.learnable.viewmodel.RecordingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecordingFragment : Fragment() {
    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordingViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()

        checkAndRequestPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recordingState.collectLatest { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transcriptionResult.collectLatest { result ->
                result?.let {
                    binding.transcriptionLabel.visibility = View.VISIBLE
                    binding.transcriptionText.visibility = View.VISIBLE
                    binding.transcriptionText.text = it
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.recordButton.setOnClickListener {
            when (viewModel.recordingState.value) {
                is RecordingState.Idle -> viewModel.startRecording()
                is RecordingState.Recording -> viewModel.stopRecording()
                else -> { /* Do nothing */ }
            }
        }

        binding.cancelButton.setOnClickListener {
            viewModel.cancelRecording()
        }
    }

    private fun updateUI(state: RecordingState) {
        binding.statusText.text = when (state) {
            is RecordingState.Idle -> "Press record to start"
            is RecordingState.Recording -> "Recording..."
            is RecordingState.Transcribing -> "Transcribing..."
        }

        binding.recordButton.text = when (state) {
            is RecordingState.Idle -> "Start Recording"
            is RecordingState.Recording -> "Stop Recording"
            is RecordingState.Transcribing -> "Transcribing..."
        }

        binding.recordButton.isEnabled = state !is RecordingState.Transcribing
        binding.cancelButton.visibility = if (state is RecordingState.Recording) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
} 