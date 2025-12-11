package com.adika.learnable.view.dashboard.student.material

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.FragmentPdfViewerBinding
import com.adika.learnable.view.core.BaseFragment
import com.rajat.pdfviewer.PdfRendererView
import com.rajat.pdfviewer.util.CacheStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class PdfViewerFragment : BaseFragment() {
    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!
    private val args: PdfViewerFragmentArgs by navArgs()

    private var pdfUrl: String? = null
    private var currentPage: Int = 1
    private var totalPages: Int = 0
    private var onPdfCompletedListener: (() -> Unit)? = null
    private var isRetrying: Boolean = false
    private var cachedPdfFile: File? = null

    fun setOnPdfCompletedListener(listener: () -> Unit) {
        onPdfCompletedListener = listener
    }

    private val pdfStatusListener = object : PdfRendererView.StatusCallBack {
        override fun onPageChanged(currentPage: Int, totalPage: Int) {
            this@PdfViewerFragment.currentPage = currentPage + 1
            this@PdfViewerFragment.totalPages = totalPage

            if (currentPage + 1 == totalPage) {
                onPdfCompletedListener?.invoke()
            }
        }

        override fun onPdfLoadStart() {
            showLoading(true)
            showErrorState(false)
        }

        override fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {
            updateDownloadProgress(progress, downloadedBytes, totalBytes)
        }

        override fun onPdfLoadSuccess(absolutePath: String) {
            showLoading(false)
            showErrorState(false)
            cachedPdfFile = File(absolutePath)
            Log.d(TAG, "PDF loaded successfully: $absolutePath")

            setupScrollListener()
        }

        override fun onError(error: Throwable) {
            showLoading(false)
            handlePdfLoadError(error)
        }
    }

    private val zoomListener = object : PdfRendererView.ZoomListener {
        override fun onZoomChanged(isZoomedIn: Boolean, scale: Float) {
            Log.d(TAG, "Zoom changed - Zoomed: $isZoomedIn, Scale: $scale")

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPdfViewer()
        setupErrorHandling()
        setupTitle()

        pdfUrl = args.pdfUrl
        pdfUrl?.let { loadPdf(it) }

        setupTextScaling()
    }

    private fun setupPdfViewer() {
        binding.pdfViewer.apply {
            zoomListener = this@PdfViewerFragment.zoomListener
            statusListener = pdfStatusListener
        }
    }

    private fun setupTitle() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupScrollListener() {
        try {
            binding.pdfViewer.recyclerView.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val firstVisiblePosition = it.findFirstVisibleItemPosition()
                        currentPage = firstVisiblePosition + 1

                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up scroll listener", e)
        }
    }

    private fun setupErrorHandling() {
        binding.errorLayout.retryButton.setOnClickListener {
            if (!isRetrying) {
                isRetrying = true
                pdfUrl?.let { loadPdf(it) }
            }
        }
    }

    private fun loadPdf(url: String) {
        pdfUrl = url
        showLoading(true)
        showErrorState(false)

        binding.pdfViewer.initWithUrl(
            url = url,
            lifecycleCoroutineScope = lifecycleScope,
            lifecycle = viewLifecycleOwner.lifecycle,
            cacheStrategy = CacheStrategy.MAXIMIZE_PERFORMANCE
        )
    }

    fun getCurrentPage(): Int = currentPage

    fun getTotalPages(): Int = totalPages

    fun jumpToPage(pageNumber: Int) {
        if (pageNumber in 1..totalPages) {
            binding.pdfViewer.jumpToPage(pageNumber - 1)

        } else {
            showError("Halaman tidak valid")
        }
    }

    fun isZoomedIn(): Boolean = binding.pdfViewer.isZoomedIn()


    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

    }

    private fun updateDownloadProgress(progress: Int, downloaded: Long, total: Long?) {
        binding.progressBar.progress = progress
        binding.progressText.text = when {
            total != null -> {
                val downloadedMB = downloaded / (1024 * 1024)
                val totalMB = total / (1024 * 1024)
                "$downloadedMB MB / $totalMB MB"
            }

            else -> "$progress%"
        }
    }

    private fun handlePdfLoadError(error: Throwable) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                Log.e(TAG, "PDF Load Error: ${error.message}", error)
                showErrorState(true)
                binding.errorLayout.errorMessage.text = when (error) {
                    is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                    is java.io.FileNotFoundException -> "File PDF tidak ditemukan."
                    else -> "Gagal memuat PDF: ${error.message}"
                }
            }
        }
    }

    private fun showErrorState(show: Boolean) {
        binding.errorLayout.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.pdfViewer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.pdfViewer.closePdfRender()
        _binding = null
    }

    companion object {
        private const val TAG = "PdfViewerFragment"

        fun newInstance(url: String? = null): PdfViewerFragment {
            return PdfViewerFragment().apply {
                pdfUrl = url
            }
        }
    }
}