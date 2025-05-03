package com.adika.learnable.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.adika.learnable.databinding.ActivityRecordingBinding
import com.adika.learnable.viewmodel.RecordingState
import com.adika.learnable.viewmodel.RecordingViewModel

class RecordingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordingBinding
    private lateinit var viewModel: RecordingViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RecordingViewModel::class.java]

        setupObservers()
        setupClickListeners()

        checkAndRequestPermission()
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
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
        viewModel.recordingState.observe(this) { state ->
            updateUI(state)
        }

        viewModel.transcriptionResult.observe(this) { result ->
            result?.let {
                binding.transcriptionLabel.visibility = android.view.View.VISIBLE
                binding.transcriptionText.visibility = android.view.View.VISIBLE
                binding.transcriptionText.text = it
            }
        }
    }

    private fun setupClickListeners() {
        binding.recordButton.setOnClickListener {
            when (viewModel.recordingState.value) {
                is RecordingState.Idle -> viewModel.startRecording()
                is RecordingState.Recording -> viewModel.stopRecording()
                else -> { /* Do nothing */
                }
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
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
} 