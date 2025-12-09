package com.adika.learnable.view.dashboard.student.material

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.adika.learnable.databinding.FragmentAudioPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioPlayerFragment : Fragment() {
    private var _binding: FragmentAudioPlayerBinding? = null
    private val binding get() = _binding!!
    private var audioPlayer: ExoPlayer? = null
    private var audioUrl: String? = null
    private var onAudioCompleted: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePlayer()
        audioUrl?.let { playAudio(it) }
    }

    private fun initializePlayer() {
        audioPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onAudioCompleted?.invoke()
                    }
                }
            })
        }
        binding.audioPlayer.player = audioPlayer
    }

    fun playAudio(url: String) {
        audioUrl = url
        audioPlayer?.setMediaItem(MediaItem.fromUri(url))
        audioPlayer?.prepare()
        audioPlayer?.play()
    }

    fun setOnAudioCompletedListener(listener: () -> Unit) {
        onAudioCompleted = listener
    }

    override fun onPause() {
        super.onPause()
        audioPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioPlayer?.release()
        _binding = null
    }

    companion object {
        fun newInstance(url: String? = null): AudioPlayerFragment {
            return AudioPlayerFragment().apply {
                this.audioUrl = url
            }
        }
    }
} 