package com.adika.learnable.view.dashboard.student

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.HorizontalSubBabProgressAdapter
import com.adika.learnable.adapter.StudentSubjectAdapter
import com.adika.learnable.adapter.VideoRecommendationAdapter
import com.adika.learnable.databinding.FragmentStudentDashboardBinding
import com.adika.learnable.model.User
import com.adika.learnable.util.AvatarUtils.getInitial
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.util.GridSpacingItemDecoration
import com.adika.learnable.util.loadAvatar
import com.adika.learnable.util.setupAccessibilityButton
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.view.dashboard.student.dialog.ChooseClassDialog
import com.adika.learnable.viewmodel.dashboard.StudentDashboardViewModel
import com.adika.learnable.viewmodel.settings.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StudentDashboardFragment : BaseFragment() {
    private var _binding: FragmentStudentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudentDashboardViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var studentSubjectAdapter: StudentSubjectAdapter
    private lateinit var subBabProgressAdapter: HorizontalSubBabProgressAdapter
    private lateinit var videoRecommendationAdapter: VideoRecommendationAdapter

    private val textScalePreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "text_scale") {
                view?.post {
                    applyTextScale()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupSubjects()
        setupSubBabProgress()
        setupVideoRecommendations()
        setupClickListeners()
        loadProfileData()
        setupNotificationBadge()
        setupAccessibilityButton() // Setup accessibility button after other components

        initializeLoadingIndicators()

        val schoolLevel = viewModel.selectedSchoolLevel.value ?: "sd"
        viewModel.loadSubjectsBySchoolLevel(schoolLevel)
        viewModel.loadSubBabProgress()
        viewModel.loadRandomVideoRecommendations()

        setupTextScaling()

        parentFragmentManager.setFragmentResultListener(
            "text_scale_changed",
            viewLifecycleOwner
        ) { _, _ ->
            view.post {
                applyTextScale()
            }
        }

        val prefs = requireContext().getSharedPreferences(
            "accessibility_prefs",
            android.content.Context.MODE_PRIVATE
        )
        prefs.registerOnSharedPreferenceChangeListener(textScalePreferenceListener)
    }

    private fun initializeLoadingIndicators() {
        binding.progressBarSubjects.visibility = View.GONE
        binding.progressBarSubBab.visibility = View.GONE
        binding.progressBarVideo.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        val currentState = viewModel.subjectsState.value
        if (currentState !is StudentDashboardViewModel.SubjectState.Success ||
            currentState.subject.isNullOrEmpty()
        ) {
            viewModel.selectedSchoolLevel.value?.let {
                Log.d("StudentDashboard", "Reloading subjects on resume")
                viewModel.reloadLastSelectedLevel()
            }
        }
    }

    private fun setupSubjects() {
        val spanCount = resources.getInteger(R.integer.grid_span_subjects)
        val spacingPx = resources.getDimensionPixelSize(R.dimen.grid_spacing_12)

        val glm = GridLayoutManager(requireContext(), spanCount)
        binding.rvSubjects.layoutManager = glm
        binding.rvSubjects.setHasFixedSize(true)
        binding.rvSubjects.isNestedScrollingEnabled = false
        binding.rvSubjects.setPadding(0, 0, 0, 0)
        binding.rvSubjects.clipToPadding = false

        if (binding.rvSubjects.itemDecorationCount == 0) {
            binding.rvSubjects.addItemDecoration(
                GridSpacingItemDecoration(glm.spanCount, spacingPx, includeEdge = true)
            )
        }

        studentSubjectAdapter = StudentSubjectAdapter { subject ->
            val state = viewModel.studentState.value

            if (state is StudentDashboardViewModel.StudentState.Success) {
                showLoading(false)
                val action = StudentDashboardFragmentDirections
                    .actionStudentDashboardToLessonList(
                        idSubject = subject.idSubject,
                        subjectName = subject.name,
                        schoolLevel = subject.schoolLevel
                    )
                findNavController().navigate(action)

            } else {
                showToast(getString(R.string.fail_load_user_data))
            }
        }
        binding.rvSubjects.adapter = studentSubjectAdapter

        studentSubjectAdapter.submitList(emptyList())
    }

    private fun setupSubBabProgress() {

        subBabProgressAdapter = HorizontalSubBabProgressAdapter()
        binding.rvSubbabProgress.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSubbabProgress.setHasFixedSize(true)
        binding.rvSubbabProgress.isNestedScrollingEnabled = false

        binding.rvSubbabProgress.adapter = subBabProgressAdapter

        subBabProgressAdapter.submitList(emptyList())
    }

    private fun setupVideoRecommendations() {
        videoRecommendationAdapter = VideoRecommendationAdapter { item ->
            val action = StudentDashboardFragmentDirections
                .actionStudentDashboardFragmentToVideoPlayerFragment(
                    videoUrl = item.videoUrl,
                    subtitleUrl = null
                )
            findNavController().navigate(action)
        }

        binding.rvSubbabProgress.setHasFixedSize(true)
        binding.rvSubbabProgress.isNestedScrollingEnabled = false

        binding.rvVideoRecommendations.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = videoRecommendationAdapter
        }

        videoRecommendationAdapter.submitList(emptyList())
    }


    private fun setupClickListeners() {
        binding.apply {
            cardStudyCalendar.setOnClickListener {
                findNavController().navigate(R.id.action_studentDashboard_to_progressFragment)
            }

            btnNextProgress.setOnClickListener {
                findNavController().navigate(R.id.action_studentDashboard_to_progressFragment)
            }

            btnChooseClass.setOnClickListener {
                showClassFilterDialog()
            }

            ivNotification.setOnClickListener {
                findNavController().navigate(R.id.action_studentDashboard_to_notificationFragment)
            }

            tvViewAllProgress.setOnClickListener {
                findNavController().navigate(R.id.action_studentDashboard_to_progressFragment)
            }

            tvViewAllSubjects.setOnClickListener {
                findNavController().navigate(R.id.action_studentDashboard_to_subjectFragment)
            }

        }

        setupSearchFunctionality()
    }

    private fun setupAccessibilityButton() {
        setupAccessibilityButton(binding.btnAccessibility)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun applyTextScale() {
        super.applyTextScale()

        if (::studentSubjectAdapter.isInitialized) {
            Log.d("StudentDashboard", "Refreshing subject adapter")
            studentSubjectAdapter.notifyDataSetChanged()
        }
        if (::subBabProgressAdapter.isInitialized) {
            Log.d("StudentDashboard", "Refreshing subbab adapter")
            subBabProgressAdapter.notifyDataSetChanged()
        }
        if (::videoRecommendationAdapter.isInitialized) {
            Log.d("StudentDashboard", "Refreshing video adapter")
            videoRecommendationAdapter.notifyDataSetChanged()
        }
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchSubjects(s?.toString() ?: "")
            }
        })
    }

    private fun showClassFilterDialog() {
        val currentOptions = viewModel.sortFilterOptions.value
        val dialog = ChooseClassDialog.newInstance(
            currentOptions = currentOptions,
            onClassFilterApplied = { options ->
                viewModel.applySortFilter(options)
            }
        )
        dialog.show(parentFragmentManager, "ChooseClassDialog")
    }

    private fun observeViewModel() {
        viewModel.studentState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.subjectsState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.selectedSchoolLevel.observe(viewLifecycleOwner) { level ->
            updateClassButtonText(level)
        }

        viewModel.sortFilterOptions.observe(viewLifecycleOwner) {
            updateClassButtonText(viewModel.selectedSchoolLevel.value)
        }

        viewModel.subBabProgressState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.videoRecsState.observe(viewLifecycleOwner) { video ->
            handleState(video)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is StudentDashboardViewModel.StudentState.Loading -> {
                showLoading(true)
            }

            is StudentDashboardViewModel.SubjectState.Loading -> {
                showSubjectsLoading(true)
            }

            is StudentDashboardViewModel.SubBabProgressState.Loading -> {
                showProgressLoading(true)
            }

            is StudentDashboardViewModel.VideoRecsState.Loading -> {
                showVideoLoading(true)
            }

            is StudentDashboardViewModel.StudentState.Success -> {
                updateUI(state.student)
            }

            is StudentDashboardViewModel.SubjectState.Success -> {
                showSubjectsLoading(false)
                state.subject?.let { subjects ->
                    studentSubjectAdapter.submitList(subjects) {

                        binding.rvSubjects.requestLayout()
                    }

                    if (subjects.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                    }
                } ?: run {
                    studentSubjectAdapter.submitList(emptyList())
                    showEmptyState(true)
                }
            }

            is StudentDashboardViewModel.SubBabProgressState.Success -> {
                showProgressLoading(false)
                subBabProgressAdapter.submitList(state.progressItems)

                if (state.progressItems.isEmpty()) {
                    showEmptyProgressState(true)
                } else {
                    showEmptyProgressState(false)
                }
            }

            is StudentDashboardViewModel.VideoRecsState.Success -> {
                showVideoLoading(false)
                videoRecommendationAdapter.submitList(state.items)
            }

            is StudentDashboardViewModel.StudentState.Error -> {
                // Student error doesn't need separate loading indicator
            }

            is StudentDashboardViewModel.SubjectState.Error -> {
                showSubjectsLoading(false)
                val errorMessage = state.message ?: getString(R.string.unknown_error)
                showToast(errorMessage)
            }

            is StudentDashboardViewModel.SubBabProgressState.Error -> {
                showProgressLoading(false)
                val errorMessage = state.message ?: getString(R.string.unknown_error)
                showToast(errorMessage)
                showEmptyProgressState(true)
            }

            is StudentDashboardViewModel.VideoRecsState.Error -> {
                showVideoLoading(false)
                val errorMessage = state.message ?: getString(R.string.unknown_error)
                showToast(errorMessage)
            }
        }
    }

    private fun updateUI(user: User) {
        binding.apply {
            tvGreeting.text = getString(R.string.greeting, user.name)
            ivProfile.loadAvatar(
                name = user.name,
                photoUrl = user.profilePicture
            )
        }
    }

    private fun loadProfileData() {
        viewModel.loadUserData()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.root.isEnabled = !isLoading

        if (isLoading) {
            binding.rvSubjects.visibility = View.GONE
        } else {
            binding.rvSubjects.visibility = View.VISIBLE
        }
    }

    private fun showSubjectsLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarSubjects.visibility = View.VISIBLE
            binding.rvSubjects.visibility = View.GONE
        } else {
            binding.progressBarSubjects.visibility = View.GONE
            binding.rvSubjects.visibility = View.VISIBLE
        }
    }

    private fun showProgressLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarSubBab.visibility = View.VISIBLE
            binding.rvSubbabProgress.visibility = View.GONE
            binding.emptyProgress.visibility = View.GONE
        } else {
            binding.progressBarSubBab.visibility = View.GONE
            binding.rvSubbabProgress.visibility = View.VISIBLE
        }
    }

    private fun showVideoLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBarVideo.visibility = View.VISIBLE
            binding.rvVideoRecommendations.visibility = View.GONE
        } else {
            binding.progressBarVideo.visibility = View.GONE
            binding.rvVideoRecommendations.visibility = View.VISIBLE
        }
    }

    private fun updateClassButtonText(level: String?) {
        val currentOptions = viewModel.sortFilterOptions.value
        val sd = getInitial(getString(R.string.elementarySchool))
        val smp = getInitial(getString(R.string.juniorHighSchool))
        val sma = getInitial(getString(R.string.seniorHighSchool))
        val buttonText = when (currentOptions?.filterBy) {
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSES -> sd
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSJHS -> smp
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSSHS -> sma
            null -> when (level) {
                EducationLevels.SD -> sd
                EducationLevels.SMP -> smp
                EducationLevels.SMA -> sma
                else -> getString(R.string.choose_level)
            }
        }
        binding.btnChooseClass.text = buttonText
    }

    private fun showEmptyState(isEmpty: Boolean) {

        // For now, we'll just show a toast message
        if (isEmpty) {
            Toast.makeText(
                requireContext(),
                "Tidak ada mata pelajaran ditemukan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showEmptyProgressState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                emptyProgress.visibility = View.VISIBLE
                rvSubbabProgress.visibility = View.GONE
            } else {
                emptyProgress.visibility = View.GONE
                rvSubbabProgress.visibility = View.VISIBLE
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupNotificationBadge() {

        notificationViewModel.getUnreadCount()

        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateNotificationBadge(count)
        }
    }

    private fun updateNotificationBadge(count: Int) {
        if (count > 0) {

            binding.ivNotification.setImageResource(R.drawable.ic_notification_badge)

        } else {

            binding.ivNotification.setImageResource(R.drawable.ic_notification)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val prefs = requireContext().getSharedPreferences(
            "accessibility_prefs",
            android.content.Context.MODE_PRIVATE
        )
        prefs.unregisterOnSharedPreferenceChangeListener(textScalePreferenceListener)

        _binding = null
    }
}