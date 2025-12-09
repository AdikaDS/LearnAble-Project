package com.adika.learnable.view.dashboard.student.material

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.adika.learnable.databinding.FragmentVideoPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerFragment : Fragment() {
    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var videoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var onVideoCompleted: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePlayer()
        videoUrl?.let { playVideo(it) }
    }

    private fun initializePlayer() {
        videoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            playWhenReady = true // Ubah ke true agar langsung play
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_ENDED -> {
                            onVideoCompleted?.invoke()
                        }
                    }
                }
            })
        }
        binding.videoPlayer.player = videoPlayer
    }

    private fun playVideo(url: String) {
        try {
            videoPlayer?.setMediaItem(MediaItem.fromUri(url))
            videoPlayer?.prepare()
            videoPlayer?.play()
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error playing video", e)
            Toast.makeText(
                context,
                "Gagal memutar video: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setOnVideoCompletedListener(listener: () -> Unit) {
        onVideoCompleted = listener
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        videoPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoPlayer?.release()
        _binding = null
    }

    companion object {
        fun newInstance(url: String? = null): VideoPlayerFragment {
            return VideoPlayerFragment().apply {
                this.videoUrl = url
            }
        }
    }
}