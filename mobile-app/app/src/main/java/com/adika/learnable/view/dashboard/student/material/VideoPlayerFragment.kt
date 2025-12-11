package com.adika.learnable.view.dashboard.student.material

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentVideoPlayerBinding
import com.adika.learnable.view.core.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@UnstableApi
@AndroidEntryPoint
class VideoPlayerFragment : BaseFragment() {
    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private val args: VideoPlayerFragmentArgs by navArgs()
    private var videoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var onVideoCompleted: (() -> Unit)? = null
    private var isInPictureInPictureMode = false
    private var wasPlayingBeforePiP = false
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var controlsVisible = true
    private var controlsHandler: Handler? = null
    private var hideControlsRunnable: Runnable? = null
    private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    private var isVideoEnded: Boolean = false

    private val ACTION_PIP_REWIND = "com.adika.learnable.PIP_REWIND"
    private val ACTION_PIP_PLAY = "com.adika.learnable.PIP_PLAY"
    private val ACTION_PIP_PAUSE = "com.adika.learnable.PIP_PAUSE"
    private val ACTION_PIP_FFWD = "com.adika.learnable.PIP_FFWD"
    private var pipReceiverRegistered = false

    private val pipActionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_REWIND -> videoPlayer?.seekTo(
                    (videoPlayer?.currentPosition ?: 0) - 10000
                )

                ACTION_PIP_PLAY -> {
                    if (isVideoEnded) {
                        videoPlayer?.seekTo(0)
                        isVideoEnded = false
                    }
                    videoPlayer?.play()
                    updatePipActions()
                }

                ACTION_PIP_PAUSE -> {
                    videoPlayer?.pause()
                    updatePipActions()
                }

                ACTION_PIP_FFWD -> videoPlayer?.seekTo((videoPlayer?.currentPosition ?: 0) + 10000)
            }
        }
    }

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
        setupBackPressHandler()
        initializePlayer()
        setupPlayerControls()
        setupProgressUpdater()
        setupControlsVisibility()

        allowOrientationChanges()

        if (args.videoUrl.isNotEmpty()) {
            playVideo(args.videoUrl, args.subtitleUrl)
        }

        setupTextScaling()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isInPictureInPictureMode) {

                        exitPictureInPictureMode()
                    } else {

                        handleBackButtonPress()
                    }
                }
            }
        )
    }

    private fun initializePlayer() {
        trackSelector = DefaultTrackSelector(requireContext()).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredVideoMimeType("video/mp4")
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setForceHighestSupportedBitrate(false) // Allow adaptive bitrate for faster loading
                    .setExceedVideoConstraintsIfNecessary(true) // Allow exceeding constraints
            )
        }

        videoPlayer = ExoPlayer.Builder(requireContext())
            .setTrackSelector(trackSelector!!)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF

                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_ENDED -> {
                                isVideoEnded = true
                                onVideoCompleted?.invoke()
                                updatePlayPauseUiForEnded()
                                if (isInPictureInPictureMode) updatePipActions()
                            }

                            Player.STATE_BUFFERING -> {
                                showLoading(true)

                            }

                            Player.STATE_READY -> {
                                showLoading(false)
                                startProgressUpdater()
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isInPictureInPictureMode) updatePipActions()

                        if (isPlaying) {
                            startProgressUpdater()
                        } else {
                            stopProgressUpdater()
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {

                        handleSubtitleTracks(tracks)
                    }
                })
            }

        binding.videoPlayer.player = videoPlayer
    }

    private fun updatePlayPauseUiForEnded() {

        setPlayPauseUi(isPlaying = false)

        val topControls = binding.videoPlayer.findViewById<View>(R.id.top_controls)
        val centerControls = binding.videoPlayer.findViewById<View>(R.id.center_controls)
        val bottomControls = binding.videoPlayer.findViewById<View>(R.id.bottom_controls)
        val bottomControlsLandscape =
            binding.videoPlayer.findViewById<View>(R.id.bottom_controls_landscape)

        if (isInPictureInPictureMode) {

            topControls?.apply { alpha = 0f; visibility = View.GONE }
            centerControls?.apply { alpha = 0f; visibility = View.GONE }
            bottomControls?.apply { alpha = 0f; visibility = View.GONE }
            bottomControlsLandscape?.apply { alpha = 0f; visibility = View.GONE }
            controlsVisible = false
            cancelAutoHideControls()
            return
        }

        topControls?.apply { alpha = 1f; visibility = View.VISIBLE }

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            centerControls?.apply { alpha = 1f; visibility = View.VISIBLE }
            bottomControlsLandscape?.apply { alpha = 1f; visibility = View.VISIBLE }
            bottomControls?.visibility = View.GONE
        } else {
            centerControls?.visibility = View.GONE
            bottomControls?.apply { alpha = 1f; visibility = View.VISIBLE }
            bottomControlsLandscape?.visibility = View.GONE
        }

        binding.videoPlayer.showController()
        controlsVisible = true
        cancelAutoHideControls()
    }

    private fun setPlayPauseUi(isPlaying: Boolean) {
        val portraitPlayButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_portrait)
        val portraitPauseButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_portrait)
        val centerPlayButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_center)
        val centerPauseButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_center)
        val playLandscapeButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_landscape)
        val pauseLandscapeButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_landscape)

        if (isPlaying) {
            portraitPlayButton?.visibility = View.GONE
            portraitPauseButton?.visibility = View.VISIBLE
            centerPlayButton?.visibility = View.GONE
            centerPauseButton?.visibility = View.VISIBLE
            playLandscapeButton?.visibility = View.GONE
            pauseLandscapeButton?.visibility = View.VISIBLE
        } else {
            portraitPlayButton?.visibility = View.VISIBLE
            portraitPauseButton?.visibility = View.GONE
            centerPlayButton?.visibility = View.VISIBLE
            centerPauseButton?.visibility = View.GONE
            playLandscapeButton?.visibility = View.VISIBLE
            pauseLandscapeButton?.visibility = View.GONE
        }
    }

    private fun setupPlayerControls() {
        binding.videoPlayer.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        binding.videoPlayer.setControllerShowTimeoutMs(3000)
        binding.videoPlayer.setControllerHideOnTouch(false)

        binding.videoPlayer.setUseController(true)

        setupCustomControls()
    }

    private fun setupCustomControls() {

        val backButton = binding.videoPlayer.findViewById<View>(R.id.exo_back)

        val portraitPlayButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_portrait)
        val portraitPauseButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_portrait)
        val screenSizeButton = binding.videoPlayer.findViewById<View>(R.id.screen_size_button)

        val rewindCenterButton = binding.videoPlayer.findViewById<View>(R.id.exo_rew_center)
        val forwardCenterButton = binding.videoPlayer.findViewById<View>(R.id.exo_ffwd_center)
        val centerPlayButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_center)
        val centerPauseButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_center)

        val playLandscapeButton = binding.videoPlayer.findViewById<View>(R.id.exo_play_landscape)
        val pauseLandscapeButton = binding.videoPlayer.findViewById<View>(R.id.exo_pause_landscape)
        val pipButton = binding.videoPlayer.findViewById<View>(R.id.pip_button)
        val languageButton = binding.videoPlayer.findViewById<View>(R.id.language_button)

        backButton?.setOnClickListener {
            handleBackButtonPress()
        }

        val topControls = binding.videoPlayer.findViewById<View>(R.id.top_controls)
        val centerControls = binding.videoPlayer.findViewById<View>(R.id.center_controls)
        val bottomControls = binding.videoPlayer.findViewById<View>(R.id.bottom_controls)
        val bottomControlsLandscape =
            binding.videoPlayer.findViewById<View>(R.id.bottom_controls_landscape)

        topControls?.setOnClickListener { /* Do nothing - prevent event bubbling */ }
        centerControls?.setOnClickListener { /* Do nothing - prevent event bubbling */ }
        bottomControls?.setOnClickListener { /* Do nothing - prevent event bubbling */ }
        bottomControlsLandscape?.setOnClickListener { /* Do nothing - prevent event bubbling */ }

        portraitPlayButton?.setOnClickListener {
            if (isVideoEnded) {

                videoPlayer?.seekTo(0)
                isVideoEnded = false
            }
            videoPlayer?.play()
            setPlayPauseUi(isPlaying = true)

            startAutoHideControls()
        }

        portraitPauseButton?.setOnClickListener {
            videoPlayer?.pause()
            setPlayPauseUi(isPlaying = false)

            startAutoHideControls()
        }

        screenSizeButton?.setOnClickListener {
            toggleFullscreen()

            startAutoHideControls()
        }


        rewindCenterButton?.setOnClickListener {
            videoPlayer?.seekTo((videoPlayer?.currentPosition ?: 0) - 10000)

            startAutoHideControls()
        }

        forwardCenterButton?.setOnClickListener {
            videoPlayer?.seekTo((videoPlayer?.currentPosition ?: 0) + 10000)

            startAutoHideControls()
        }

        centerPlayButton?.setOnClickListener {
            if (isVideoEnded) {
                videoPlayer?.seekTo(0)
                isVideoEnded = false
            }
            videoPlayer?.play()
            setPlayPauseUi(isPlaying = true)

            startAutoHideControls()
        }

        centerPauseButton?.setOnClickListener {
            videoPlayer?.pause()
            setPlayPauseUi(isPlaying = false)

            startAutoHideControls()
        }

        playLandscapeButton?.setOnClickListener {
            if (isVideoEnded) {
                videoPlayer?.seekTo(0)
                isVideoEnded = false
            }
            videoPlayer?.play()
            setPlayPauseUi(isPlaying = true)

            startAutoHideControls()
        }

        pauseLandscapeButton?.setOnClickListener {
            videoPlayer?.pause()
            setPlayPauseUi(isPlaying = false)

            startAutoHideControls()
        }

        pipButton.setOnClickListener {

            enterPictureInPictureMode()

            startAutoHideControls()
        }

        languageButton?.setOnClickListener {
            toggleSubtitles()

            startAutoHideControls()
        }

        updateControlVisibility(resources.configuration.orientation)
    }

    private fun setupControlsVisibility() {
        controlsHandler = Handler(Looper.getMainLooper())
        hideControlsRunnable = Runnable {
            hideControls()
        }

        binding.videoPlayer.setOnClickListener {
            toggleControlsVisibility()
        }

        startAutoHideControls()
    }

    private fun toggleControlsVisibility() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        if (!controlsVisible) {
            controlsVisible = true

            val topControls = binding.videoPlayer.findViewById<View>(R.id.top_controls)
            topControls?.animate()?.alpha(1f)?.setDuration(300)?.start()

            val centerControls = binding.videoPlayer.findViewById<View>(R.id.center_controls)
            centerControls?.animate()?.alpha(1f)?.setDuration(300)?.start()

            val bottomControls = binding.videoPlayer.findViewById<View>(R.id.bottom_controls)
            bottomControls?.animate()?.alpha(1f)?.setDuration(300)?.start()

            val bottomControlsLandscape =
                binding.videoPlayer.findViewById<View>(R.id.bottom_controls_landscape)
            bottomControlsLandscape?.animate()?.alpha(1f)?.setDuration(300)?.start()

            startAutoHideControls()
        }
    }

    private fun hideControls() {
        if (controlsVisible) {
            controlsVisible = false

            val topControls = binding.videoPlayer.findViewById<View>(R.id.top_controls)
            topControls?.animate()?.alpha(0f)?.setDuration(300)?.start()

            val centerControls = binding.videoPlayer.findViewById<View>(R.id.center_controls)
            centerControls?.animate()?.alpha(0f)?.setDuration(300)?.start()

            val bottomControls = binding.videoPlayer.findViewById<View>(R.id.bottom_controls)
            bottomControls?.animate()?.alpha(0f)?.setDuration(300)?.start()

            val bottomControlsLandscape =
                binding.videoPlayer.findViewById<View>(R.id.bottom_controls_landscape)
            bottomControlsLandscape?.animate()?.alpha(0f)?.setDuration(300)?.start()

            cancelAutoHideControls()
        }
    }

    private fun startAutoHideControls() {
        hideControlsRunnable?.let { runnable ->
            controlsHandler?.removeCallbacks(runnable)
            controlsHandler?.postDelayed(runnable, 3000) // Hide after 3 seconds
        }
    }

    private fun cancelAutoHideControls() {
        hideControlsRunnable?.let { runnable ->
            controlsHandler?.removeCallbacks(runnable)
        }
    }

    private fun handleBackButtonPress() {
        val activity = requireActivity()
        val currentOrientation = activity.requestedOrientation

        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {

            findNavController().popBackStack()
        }
    }

    private fun setupProgressUpdater() {
        progressHandler = Handler(Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                updateProgress()

                progressHandler?.postDelayed(this, 1000)
            }
        }

        setupProgressBarListeners()
    }

    private fun setupProgressBarListeners() {

        val progressBar =
            binding.videoPlayer.findViewById<DefaultTimeBar>(R.id.exo_progress_potrait)
        progressBar?.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                stopProgressUpdater()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {

                val durationText =
                    binding.videoPlayer.findViewById<TextView>(R.id.exo_duration_potrait)
                val currentTime = formatTime(position)
                val totalTime = formatTime(videoPlayer?.duration ?: 0)

                durationText?.text = getString(R.string.duration_video, currentTime, totalTime)
            }

            override fun onScrubStop(
                timeBar: TimeBar,
                position: Long,
                canceled: Boolean
            ) {
                if (!canceled) {
                    videoPlayer?.seekTo(position)
                }
                startProgressUpdater()
            }
        })

        val progressLandscapeBar =
            binding.videoPlayer.findViewById<DefaultTimeBar>(R.id.exo_progress_landscape)
        progressLandscapeBar?.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                stopProgressUpdater()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {

                val positionText =
                    binding.videoPlayer.findViewById<TextView>(R.id.exo_position_landscape)
                positionText?.text = formatTime(position)
            }

            override fun onScrubStop(
                timeBar: TimeBar,
                position: Long,
                canceled: Boolean
            ) {
                if (!canceled) {
                    videoPlayer?.seekTo(position)
                }
                startProgressUpdater()
            }
        })
    }

    private fun updateProgress() {
        videoPlayer?.let { player ->
            val currentPosition = player.currentPosition
            val duration = player.duration

            Log.d(
                "VideoPlayer",
                "UpdateProgress - Position: $currentPosition, Duration: $duration, State: ${player.playbackState}"
            )

            val durationText = binding.videoPlayer.findViewById<TextView>(R.id.exo_duration_potrait)
            val progressBar =
                binding.videoPlayer.findViewById<DefaultTimeBar>(R.id.exo_progress_potrait)

            val positionLandscapeText =
                binding.videoPlayer.findViewById<TextView>(R.id.exo_position_landscape)
            val durationLandscapeText =
                binding.videoPlayer.findViewById<TextView>(R.id.exo_duration_landscape)
            val progressLandscapeBar =
                binding.videoPlayer.findViewById<DefaultTimeBar>(R.id.exo_progress_landscape)

            val currentTime = formatTime(currentPosition)
            val totalTime = if (duration != C.TIME_UNSET && duration > 0) {
                formatTime(duration)
            } else {
                "00:00"
            }

            durationText?.text = getString(R.string.duration_video, currentTime, totalTime)
            Log.d("VideoPlayer", "Updated duration text: $currentTime/$totalTime")

            positionLandscapeText?.text = currentTime
            durationLandscapeText?.text = totalTime

            if (duration != C.TIME_UNSET && duration > 0) {
                progressBar?.let { bar ->
                    bar.setPosition(currentPosition)
                    bar.setDuration(duration)
                    bar.setBufferedPosition(player.bufferedPosition)
                }

                progressLandscapeBar?.let { bar ->
                    bar.setPosition(currentPosition)
                    bar.setDuration(duration)
                    bar.setBufferedPosition(player.bufferedPosition)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(timeMs: Long): String {
        if (timeMs == C.TIME_UNSET || timeMs < 0) return "00:00"

        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun startProgressUpdater() {

        stopProgressUpdater()

        progressRunnable?.let { runnable ->
            progressHandler?.post(runnable)
        }
    }

    private fun stopProgressUpdater() {
        progressRunnable?.let { runnable ->
            progressHandler?.removeCallbacks(runnable)
        }
    }

    private fun toggleSubtitles() {
        val tracks = videoPlayer?.currentTracks
        tracks?.let { trackInfo ->
            val textTracks = trackInfo.groups.filter { group ->
                group.type == C.TRACK_TYPE_TEXT
            }

            if (textTracks.isNotEmpty()) {
                val firstTextTrack = textTracks.first()
                val isSelected = firstTextTrack.isSelected

                if (isSelected) {

                    trackSelector?.buildUponParameters()
                        ?.setPreferredTextLanguage(null)
                        ?.setPreferredTextRoleFlags(0)?.let {
                            trackSelector?.setParameters(
                                it
                            )
                        }
                    binding.subtitleIndicator.visibility = View.GONE
                    showToast("Subtitle dimatikan")
                } else {

                    trackSelector?.buildUponParameters()
                        ?.setPreferredTextLanguage("id") // Indonesian
                        ?.setPreferredTextRoleFlags(C.ROLE_FLAG_CAPTION)
                        ?.let {
                            trackSelector?.setParameters(
                                it
                            )
                        }
                    binding.subtitleIndicator.visibility = View.VISIBLE
                    showToast("Subtitle diaktifkan")
                }
            } else {
                showToast("Tidak ada subtitle tersedia")
            }
        }
    }

    private fun toggleMute() {
        videoPlayer?.let { player ->
            val currentVolume = player.volume
            if (currentVolume > 0) {
                player.volume = 0f
                showToast("Audio dimatikan")
            } else {
                player.volume = 1f
                showToast("Audio dihidupkan")
            }
        }
    }

    private fun toggleFullscreen() {
        val activity = requireActivity()
        val currentOrientation = activity.requestedOrientation

        if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun playVideo(url: String, subtitleUrl: String? = null) {
        try {
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(url)
                .setMediaId("video_$url")

            subtitleUrl?.let { subtitle ->
                mediaItemBuilder.setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(subtitle.toUri())
                            .setMimeType("text/vtt")
                            .setLanguage("id")
                            .setLabel("Indonesian")
                            .build()
                    )
                )
            }

            videoPlayer?.setMediaItem(mediaItemBuilder.build())
            videoPlayer?.prepare()
            videoPlayer?.play()

        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error playing video", e)
            showToast("Gagal memutar video: ${e.localizedMessage}")
        }
    }

    private fun handleSubtitleTracks(tracks: Tracks) {
        val textTracks = tracks.groups.filter { group ->
            group.type == C.TRACK_TYPE_TEXT
        }

        if (textTracks.isNotEmpty()) {

            val firstTextTrack = textTracks.first()
            if (firstTextTrack.isSelected) {
                Log.d(
                    "VideoPlayer",
                    "Subtitle track enabled: ${firstTextTrack.mediaTrackGroup.getFormat(0).language}"
                )
                binding.subtitleIndicator.visibility = View.VISIBLE
            } else {
                binding.subtitleIndicator.visibility = View.GONE
            }
        } else {
            binding.subtitleIndicator.visibility = View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = requireActivity()
            if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                wasPlayingBeforePiP = videoPlayer?.isPlaying ?: false

                val actions = buildPipActions()

                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(actions)
                    .build()

                binding.videoPlayer.hideController()

                registerPipReceiver()

                activity.enterPictureInPictureMode(params)
                isInPictureInPictureMode = true
            } else {
                showToast("Picture-in-Picture tidak didukung di perangkat ini")
            }
        } else {
            showToast("Picture-in-Picture memerlukan Android 8.0 atau lebih tinggi")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipActions(): List<RemoteAction> {
        val context = requireContext()

        fun pending(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(action)
            intent.setPackage(context.packageName)
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        val rewind = RemoteAction(
            Icon.createWithResource(context, R.drawable.ic_replay_10),
            "Rewind",
            "Rewind 10 seconds",
            pending(ACTION_PIP_REWIND, 1)
        )

        val isPlaying = (videoPlayer?.isPlaying == true) && !isVideoEnded
        val playPause = if (isPlaying) {
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_pause_white),
                "Pause",
                "Pause",
                pending(ACTION_PIP_PAUSE, 2)
            )
        } else {
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_play_arrow_white),
                "Play",
                if (isVideoEnded) "Replay" else "Play",
                pending(ACTION_PIP_PLAY, 3)
            )
        }

        val forward = RemoteAction(
            Icon.createWithResource(context, R.drawable.ic_forward_10),
            "Forward",
            "Forward 10 seconds",
            pending(ACTION_PIP_FFWD, 4)
        )

        return listOf(rewind, playPause, forward)
    }

    private fun updatePipActions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activity = requireActivity()
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(buildPipActions())
        activity.setPictureInPictureParams(builder.build())
    }

    private fun registerPipReceiver() {
        if (pipReceiverRegistered) return
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_PIP_REWIND)
            addAction(ACTION_PIP_PLAY)
            addAction(ACTION_PIP_PAUSE)
            addAction(ACTION_PIP_FFWD)
        }
        ContextCompat.registerReceiver(
            requireContext(),
            pipActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        pipReceiverRegistered = true
    }

    private fun unregisterPipReceiver() {
        if (!pipReceiverRegistered) return
        runCatching { requireContext().unregisterReceiver(pipActionReceiver) }
        pipReceiverRegistered = false
    }

    fun exitPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = requireActivity()

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isInPictureInPictureMode = false
            showToast("Keluar dari mode Picture-in-Picture")

            binding.videoPlayer.showController()
            unregisterPipReceiver()
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInPictureInPictureMode = requireActivity().isInPictureInPictureMode
        }

        updateControlVisibility(newConfig.orientation)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && !isInPictureInPictureMode) {

            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        } else {

            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun updateControlVisibility(orientation: Int) {
        val centerControls = binding.videoPlayer.findViewById<View>(R.id.center_controls)
        val bottomControls = binding.videoPlayer.findViewById<View>(R.id.bottom_controls)
        val bottomControlsLandscape =
            binding.videoPlayer.findViewById<View>(R.id.bottom_controls_landscape)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

            centerControls?.visibility = View.VISIBLE
            bottomControls?.visibility = View.GONE
            bottomControlsLandscape?.visibility = View.VISIBLE
        } else {

            centerControls?.visibility = View.GONE
            bottomControls?.visibility = View.VISIBLE
            bottomControlsLandscape?.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode) {
            videoPlayer?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInPictureInPictureMode && wasPlayingBeforePiP) {
            videoPlayer?.play()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopProgressUpdater()
        cancelAutoHideControls()
        restoreOrientationLock()
        videoPlayer?.release()
        _binding = null
    }

    private fun allowOrientationChanges() {
        originalOrientation = requireActivity().requestedOrientation
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun restoreOrientationLock() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}