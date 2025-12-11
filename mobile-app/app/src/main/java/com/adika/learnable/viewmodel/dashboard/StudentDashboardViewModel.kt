package com.adika.learnable.viewmodel.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.SubBabProgressHorizontalItem
import com.adika.learnable.model.Subject
import com.adika.learnable.model.User
import com.adika.learnable.model.VideoRecommendation
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.StudentDashboardPreferencesRepository
import com.adika.learnable.repository.StudentDashboardRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubjectRepository
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.util.NormalizeSchoolLevel.formatSchoolLevel
import com.adika.learnable.util.ResourceProvider
import com.adika.learnable.view.dashboard.student.dialog.ChooseClassDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subjectRepository: SubjectRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val dashboardRepository: StudentDashboardRepository,
    private val resourceProvider: ResourceProvider,
    private val prefsRepository: StudentDashboardPreferencesRepository,
    private val textScaleRepository: TextScaleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _subjectsState = MutableLiveData<SubjectState>()
    val subjectsState: LiveData<SubjectState> = _subjectsState

    private val _allSubjects = MutableLiveData<List<Subject>>()

    private val _selectedSchoolLevel = MutableLiveData<String>()
    val selectedSchoolLevel: LiveData<String> = _selectedSchoolLevel

    private val _sortFilterOptions = MutableLiveData<ChooseClassDialog.ClassFilterOptions>()
    val sortFilterOptions: LiveData<ChooseClassDialog.ClassFilterOptions> = _sortFilterOptions

    private val _searchQuery = MutableLiveData<String>()

    private val _subBabProgressState = MutableLiveData<SubBabProgressState>()
    val subBabProgressState: LiveData<SubBabProgressState> = _subBabProgressState

    private val _textScale = MutableLiveData<Float>()
    val textScale: LiveData<Float> = _textScale

    private val _videoRecsState = MutableLiveData<VideoRecsState>()
    val videoRecsState: LiveData<VideoRecsState> = _videoRecsState

    init {
        _sortFilterOptions.value = ChooseClassDialog.ClassFilterOptions()
        loadSavedSchoolLevel()
        refreshTextScale()
    }

    fun refreshTextScale() {
        _textScale.value = textScaleRepository.getScale()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val user = authRepository.getUserData(authRepository.getCurrentUserId())
                _studentState.value = StudentState.Success(user)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_user_data)
                )
            }
        }
    }

    fun loadSubjectsBySchoolLevel(schoolLevel: String) {
        _selectedSchoolLevel.value = schoolLevel
        prefsRepository.setSelectedSchoolLevel(schoolLevel)

        viewModelScope.launch {
            _subjectsState.value = SubjectState.Loading
            try {

                val subjects = subjectRepository.getSubjectsBySchoolLevel(
                    schoolLevel = schoolLevel
                )
                _allSubjects.value = subjects
                _subjectsState.value = SubjectState.Success(subjects)
            } catch (e: Exception) {
                _subjectsState.value = SubjectState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_subjects)
                )
            }
        }
    }

    fun applySortFilter(options: ChooseClassDialog.ClassFilterOptions) {
        _sortFilterOptions.value = options
        prefsRepository.setSelectedClassFilterOrdinal(options.filterBy.ordinal)
        val schoolLevel = filterByToLevel(options.filterBy)
        loadSubjectsBySchoolLevel(schoolLevel)
    }

    fun searchSubjects(query: String) {
        _searchQuery.value = query
        val baseList = _allSubjects.value ?: emptyList()
        val filteredSubjects = if (query.isBlank()) {
            baseList
        } else {
            baseList.filter { subject ->
                subject.name.contains(query, ignoreCase = true)
            }
        }
        _subjectsState.value = SubjectState.Success(filteredSubjects)
    }

    fun reloadLastSelectedLevel() {
        _selectedSchoolLevel.value?.let { level ->
            loadSubjectsBySchoolLevel(level)
        }
    }

    fun loadSubBabProgress() {
        viewModelScope.launch {
            _subBabProgressState.value = SubBabProgressState.Loading
            try {
                val userId = authRepository.getCurrentUserId()
                val subBabProgressList =
                    studentProgressRepository.getAllStudentSubBabProgress(userId)

                val subBabProgressItems = mutableListOf<SubBabProgressHorizontalItem>()

                for (progress in subBabProgressList.take(5)) {
                    try {
                        val lesson = studentProgressRepository.getLessonById(progress.lessonId)
                        val subBab = studentProgressRepository.getSubBabById(progress.subBabId)

                        val schoolLevel = formatSchoolLevel(context, lesson?.schoolLevel ?: "")
                        if (subBab != null && lesson != null) {
                            val item = SubBabProgressHorizontalItem(
                                title = lesson.title,
                                subtitle = subBab.title,
                                schoolLevel = schoolLevel,
                                coverImage = subBab.coverImage,
                                progress = progress
                            )
                            subBabProgressItems.add(item)
                        }
                    } catch (e: Exception) {

                        continue
                    }
                }

                _subBabProgressState.value = SubBabProgressState.Success(subBabProgressItems)
            } catch (e: Exception) {
                _subBabProgressState.value = SubBabProgressState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_progress)
                )
            }
        }
    }

    fun loadRandomVideoRecommendations() {
        viewModelScope.launch {
            _videoRecsState.value = VideoRecsState.Loading
            try {
                val items = dashboardRepository.getRandomVideoRecommendations(limit = 3)
                _videoRecsState.value = VideoRecsState.Success(items)
            } catch (e: Exception) {
                _videoRecsState.value = VideoRecsState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_progress)
                )
            }
        }
    }

    private fun loadSavedSchoolLevel() {
        val savedLevel = prefsRepository.getSelectedSchoolLevel()
        val savedClassFilterOrdinal = prefsRepository.getSelectedClassFilterOrdinal()

        savedLevel?.let { level ->
            _selectedSchoolLevel.value = level
        }

        val restoredFilter =
            ChooseClassDialog.ClassFilterOptions.FilterBy.entries.getOrNull(savedClassFilterOrdinal)
                ?: levelToFilter(savedLevel)

        _sortFilterOptions.value = ChooseClassDialog.ClassFilterOptions(restoredFilter)
    }

    private fun filterByToLevel(filter: ChooseClassDialog.ClassFilterOptions.FilterBy): String {
        return when (filter) {
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSES -> EducationLevels.SD
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSJHS -> EducationLevels.SMP
            ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSSHS -> EducationLevels.SMA
        }
    }

    private fun levelToFilter(level: String?): ChooseClassDialog.ClassFilterOptions.FilterBy {
        return when (level) {
            EducationLevels.SD -> ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSES
            EducationLevels.SMP -> ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSJHS
            EducationLevels.SMA -> ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSSHS
            else -> ChooseClassDialog.ClassFilterOptions.FilterBy.CLASSES
        }
    }

    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val student: User) : StudentState()
        data class Error(val message: String) : StudentState()
    }

    sealed class SubjectState {
        data object Loading : SubjectState()
        data class Success(val subject: List<Subject>?) : SubjectState()
        data class Error(val message: String) : SubjectState()
    }

    sealed class SubBabProgressState {
        data object Loading : SubBabProgressState()
        data class Success(val progressItems: List<SubBabProgressHorizontalItem>) :
            SubBabProgressState()

        data class Error(val message: String) : SubBabProgressState()
    }

    sealed class VideoRecsState {
        data object Loading : VideoRecsState()
        data class Success(val items: List<VideoRecommendation>) : VideoRecsState()
        data class Error(val message: String) : VideoRecsState()
    }
} 