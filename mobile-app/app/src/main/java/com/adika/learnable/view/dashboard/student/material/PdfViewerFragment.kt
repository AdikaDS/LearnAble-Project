package com.adika.learnable.view.dashboard.student.material

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentPdfViewerBinding
import com.rajat.pdfviewer.PdfRendererView
import com.rajat.pdfviewer.util.CacheStrategy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class PdfViewerFragment : Fragment() {
    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!

    private var pdfUrl: String? = null
    private var currentPage: Int = 1
    private var totalPages: Int = 0
    private var onPdfCompletedListener: (() -> Unit)? = null
    private var isRetrying: Boolean = false
    private var cachedPdfFile: File? = null

    // Metode untuk set listener
    fun setOnPdfCompletedListener(listener: () -> Unit) {
        onPdfCompletedListener = listener
    }

    // Listener untuk status dan events PDF
    private val pdfStatusListener = object : PdfRendererView.StatusCallBack {
        override fun onPageChanged(currentPage: Int, totalPage: Int) {
            this@PdfViewerFragment.currentPage = currentPage + 1
            this@PdfViewerFragment.totalPages = totalPage
            updatePageIndicator()

            // Notifikasi halaman terakhir
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
            setupNavigationControls()
            setupScrollListener()
        }

        override fun onError(error: Throwable) {
            showLoading(false)
            handlePdfLoadError(error)
        }
    }

    // Zoom listener untuk tracking zoom state
    private val zoomListener = object : PdfRendererView.ZoomListener {
        override fun onZoomChanged(isZoomedIn: Boolean, scale: Float) {
            Log.d(TAG, "Zoom changed - Zoomed: $isZoomedIn, Scale: $scale")
            updateZoomControls(isZoomedIn)
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
        pdfUrl?.let { loadPdf(it) }
    }

    private fun setupPdfViewer() {
        binding.pdfViewer.apply {
            zoomListener = this@PdfViewerFragment.zoomListener
            statusListener = pdfStatusListener
        }
    }

    private fun setupScrollListener() {
        try {
            binding.pdfViewer.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val firstVisiblePosition = it.findFirstVisibleItemPosition()
                        currentPage = firstVisiblePosition + 1
                        updatePageIndicator()
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

    private fun setupNavigationControls() {
        binding.navigationLayout.apply {
            prevPageButton.setOnClickListener {
                if (currentPage > 1) {
                    jumpToPage(currentPage - 1)
                }
            }
            
            nextPageButton.setOnClickListener {
                if (currentPage < totalPages) {
                    jumpToPage(currentPage + 1)
                }
            }
        }
    }

    fun loadPdf(url: String) {
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

    // Metode untuk mendapatkan halaman saat ini
    fun getCurrentPage(): Int = currentPage

    // Metode untuk mendapatkan total halaman
    fun getTotalPages(): Int = totalPages

    // Lompat ke halaman tertentu
    fun jumpToPage(pageNumber: Int) {
        if (pageNumber in 1..totalPages) {
            binding.pdfViewer.jumpToPage(pageNumber - 1)
            updatePageIndicator()
        } else {
            showError("Halaman tidak valid")
        }
    }

    // Cek apakah sedang di-zoom
    fun isZoomedIn(): Boolean = binding.pdfViewer.isZoomedIn()

    private fun updatePageIndicator() {
        binding.navigationLayout.pageIndicator.text = "$currentPage / $totalPages"
    }

    private fun updateZoomControls(isZoomedIn: Boolean) {
        binding.navigationLayout.zoomButton.setImageResource(
            if (isZoomedIn) R.drawable.ic_zoom_out else R.drawable.ic_zoom_in
        )
    }

    // Metode utilitas untuk menampilkan loading
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.navigationLayout.root.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    // Metode untuk update progress download
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

    // Metode untuk menangani error load PDF
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

    // Metode untuk menampilkan pesan error
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