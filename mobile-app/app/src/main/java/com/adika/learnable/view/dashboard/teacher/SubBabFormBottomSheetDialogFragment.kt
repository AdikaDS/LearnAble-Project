package com.adika.learnable.view.dashboard.teacher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.adika.learnable.databinding.DialogSubBabFormBinding
import com.adika.learnable.model.SubBab
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.MaterialViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class SubBabFormBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogSubBabFormBinding? = null
    private val binding get() = _binding!!

    private var subBab: SubBab? = null
    private var lessonId: String? = null
    private var onSaveListener: ((SubBab) -> Unit)? = null
    private val viewModel: MaterialViewModel by viewModels()

    private var currentUploadType: String? = null
    private var pendingUploads = mutableMapOf<String, Pair<File, String>>() // type to (file, title)
    private var isUploading = false

    object MaterialType {
        const val VIDEO = "VIDEO"
        const val PDF = "PDF"
        const val AUDIO = "AUDIO"
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileSelection(uri)
            }
        }
    }

    companion object {
        fun newInstance(subBab: SubBab? = null, lessonId: String? = null, onSave: (SubBab) -> Unit): SubBabFormBottomSheetDialogFragment {
            return SubBabFormBottomSheetDialogFragment().apply {
                this.subBab = subBab
                this.lessonId = lessonId
                this.onSaveListener = onSave
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSubBabFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        fillExistingData()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                val newSubBab = createSubBabFromInputs()
                if (subBab == null) {
                    // Jika membuat SubBab baru
                    viewModel.createSubBabAndUploadFiles(newSubBab, pendingUploads)
                } else {
                    // Jika update SubBab yang sudah ada
                    viewModel.updateSubBabAndUploadFiles(newSubBab, pendingUploads)
                }
            }
        }

        binding.btnCancel.setOnClickListener { 
            if (!isUploading) {
                cleanup()
                dismiss()
            } else {
                showError("Mohon tunggu hingga upload selesai")
            }
        }

        // Setup upload buttons
        binding.videoSelectButton.setOnClickListener {
            if (!isUploading) {
                pickAndUploadFile(MaterialType.VIDEO)
            }
        }
        binding.audioSelectButton.setOnClickListener {
            if (!isUploading) {
                pickAndUploadFile(MaterialType.AUDIO)
            }
        }
        binding.pdfSelectButton.setOnClickListener {
            if (!isUploading) {
                pickAndUploadFile(MaterialType.PDF)
            }
        }
    }

    private fun pickAndUploadFile(type: String) {
        currentUploadType = type
        val mimeType = when (type) {
            MaterialType.VIDEO -> "video/*"
            MaterialType.AUDIO -> "audio/*"
            MaterialType.PDF -> "application/pdf"
            else -> "*/*"
        }
        
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType(mimeType)
        }
        pickFileLauncher.launch(intent)
    }

    private fun handleFileSelection(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)
            
            if (inputStream == null) {
                showError("Gagal membaca file")
                return
            }

            // Buat direktori cache jika belum ada
            val cacheDir = File(requireContext().cacheDir, "uploads")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Copy file ke cache
            val cacheFile = File(cacheDir, fileName)
            try {
                inputStream.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                showError("Gagal menyimpan file ke cache: ${e.message}")
                return
            }

            // Verifikasi file setelah copy
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                showError("File tidak tersimpan dengan benar")
                return
            }
            // Validasi file
            try {
                validateFile(cacheFile, currentUploadType ?: return)
            } catch (e: Exception) {
                showError(e.message ?: "Gagal validasi file")
                return
            }

            // Simpan file ke pendingUploads
            pendingUploads[currentUploadType ?: return] = Pair(cacheFile, fileName)

            // Update UI
            updateFileUI(currentUploadType ?: return, fileName)
            
            showSuccess("File siap untuk diupload")
        } catch (e: Exception) {
            showError("Gagal memproses file: ${e.message}")
        }
    }

    private fun validateFile(file: File, type: String) {
        if (!file.exists()) {
            throw IllegalArgumentException("File tidak ditemukan")
        }
        
        if (file.length() == 0L) {
            throw IllegalArgumentException("File kosong")
        }
        
        val maxSize = when(type) {
            MaterialType.VIDEO -> 100 * 1024 * 1024 // 100MB
            MaterialType.AUDIO -> 20 * 1024 * 1024  // 20MB
            MaterialType.PDF -> 30 * 1024 * 1024    // 30MB
            else -> 0
        }
        
        if (file.length() > maxSize) {
            throw IllegalArgumentException("Ukuran file terlalu besar. Maksimal: ${maxSize / (1024 * 1024)}MB")
        }

        // Validasi format file
        val validExtensions = when(type) {
            MaterialType.VIDEO -> listOf(".mp4", ".mov", ".avi")
            MaterialType.AUDIO -> listOf(".mp3", ".wav", ".m4a")
            MaterialType.PDF -> listOf(".pdf")
            else -> emptyList()
        }

        if (!validExtensions.any { file.name.endsWith(it, ignoreCase = true) }) {
            throw IllegalArgumentException("Format file tidak didukung. Format yang didukung: ${validExtensions.joinToString()}")
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf(File.separator)
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result ?: "unknown_file"
    }

    private fun observeViewModel() {
        viewModel.materialState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MaterialViewModel.MaterialState.Loading -> {
                    isUploading = true
                    showLoading(true)
                    disableUploadButtons()
                    binding.btnSave.isEnabled = false
                    binding.btnCancel.isEnabled = false
                }
                is MaterialViewModel.MaterialState.Success -> {
                    isUploading = false
                    showLoading(false)
                    enableUploadButtons()

                    pendingUploads.remove(state.type)
                    showSuccess("Berhasil upload ${state.title}")

                    if (pendingUploads.isEmpty()) {
                        cleanup()
                        dismiss()
                    }
                }
                is MaterialViewModel.MaterialState.Error -> {
                    isUploading = false
                    showLoading(false)
                    enableUploadButtons()
                    showError(state.message)
                }
            }
        }

        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = (progress.completed.toFloat() / progress.total * 100).toInt()
            binding.progressText.text = progress.message
        }
    }

    private fun updateFileUI(type: String, fileName: String) {
        when (type) {
            MaterialType.VIDEO -> {
                binding.videoTitleInput.setText(fileName)
            }
            MaterialType.AUDIO -> {
                binding.audioTitleInput.setText(fileName)
            }
            MaterialType.PDF -> {
                binding.pdfTitleInput.setText(fileName)
            }
        }
    }

    private fun disableUploadButtons() {
        binding.videoSelectButton.isEnabled = false
        binding.audioSelectButton.isEnabled = false
        binding.pdfSelectButton.isEnabled = false
    }

    private fun enableUploadButtons() {
        binding.pdfSelectButton.isEnabled = true
        binding.videoSelectButton.isEnabled = true
        binding.audioSelectButton.isEnabled = true
    }

    private fun fillExistingData() {
        subBab?.let {
            binding.etSubBabTitle.setText(it.title)
            binding.etSubBabDescription.setText(it.content)
            binding.etSubBabDuration.setText(it.duration.toString())
            
            // Update UI untuk file yang sudah ada
            if (it.mediaUrls["video"]?.isNotEmpty() == true) {
                updateFileUI(MaterialType.VIDEO, "File video terupload")
            }
            if (it.mediaUrls["audio"]?.isNotEmpty() == true) {
                updateFileUI(MaterialType.AUDIO, "File audio terupload")
            }
            if (it.mediaUrls["pdfLesson"]?.isNotEmpty() == true) {
                updateFileUI(MaterialType.PDF, "File PDF terupload")
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.SubBabTitleInputLayout,
                binding.etSubBabTitle,
                ValidationUtils.FieldType.TITLE
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.SubBabDescriptionInputLayout,
                binding.etSubBabDescription,
                ValidationUtils.FieldType.CONTENT
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.SubBabDurationInputLayout,
                binding.etSubBabDuration,
                ValidationUtils.FieldType.DURATION
            )
        ) isValid = false

        // Validasi file
        if (pendingUploads.isEmpty()) {
            showError("Pilih minimal satu file untuk diupload")
            isValid = false
        }

        return isValid
    }

    private fun createSubBabFromInputs(): SubBab {
        // Pertahankan mediaUrls yang ada jika sedang update
        val existingMediaUrls = subBab?.mediaUrls ?: mapOf(
            "video" to "",
            "audio" to "",
            "pdfLesson" to ""
        )
        
        return SubBab(
            id = subBab?.id ?: "",
            lessonId = subBab?.lessonId ?: lessonId ?: "",
            title = binding.etSubBabTitle.text.toString(),
            content = binding.etSubBabDescription.text.toString(),
            duration = binding.etSubBabDuration.text.toString().toIntOrNull() ?: 0,
            mediaUrls = existingMediaUrls
        )
    }

    private fun cleanup() {
        // Hapus file cache yang tidak terpakai
        pendingUploads.values.forEach { (file, _) ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("SubBabForm", "Error deleting cache file", e)
            }
        }
        pendingUploads.clear()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanup()
        _binding = null
    }
} 