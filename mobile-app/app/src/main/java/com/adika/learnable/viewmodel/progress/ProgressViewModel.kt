package com.adika.learnable.viewmodel.progress

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.StudyHistoryItem
import com.adika.learnable.model.Subject
import com.adika.learnable.model.WeekGroup
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubjectRepository
import com.adika.learnable.util.ResourceProvider
import com.adika.learnable.view.dashboard.student.progress.ProgressClassFilterDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val studentProgressRepository: StudentProgressRepository,
    private val resourceProvider: ResourceProvider,
    @ApplicationContext private val context: Context,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    private val _learnFilter = MutableLiveData(LearnFilter.HISTORY)
    val learnFilter: LiveData<LearnFilter> = _learnFilter

    private val _sortFilterClassOptions =
        MutableLiveData<ProgressClassFilterDialog.ClassFilterOptions>()
    val sortFilterClassOptions: LiveData<ProgressClassFilterDialog.ClassFilterOptions> =
        _sortFilterClassOptions

    private val _subjectsForLevel = MutableLiveData<List<Subject>>(emptyList())
    val subjectsForLevel: LiveData<List<Subject>> = _subjectsForLevel

    private val _selectedSubjects = MutableLiveData<Set<String>>(emptySet())
    val selectedSubjects: LiveData<Set<String>> = _selectedSubjects

    private val _history = MutableLiveData<List<WeekGroup>>(emptyList())
    val history: LiveData<List<WeekGroup>> = _history

    private val _activityDates = MutableLiveData<Set<LocalDate>>(emptySet())
    val activityDates: LiveData<Set<LocalDate>> = _activityDates

    fun loadTodayProgress() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val userId = studentProgressRepository.getCurrentUserId()

                val overall = studentProgressRepository.getStudentOverallProgress(userId)
                if (overall == null) {
                    _uiState.value = UiState.TodayEmpty
                } else {
                    _uiState.value = UiState.TodayHasActivity
                }
            } catch (_: Exception) {
                _uiState.value = UiState.TodayEmpty
            }
        }
    }

    fun loadRecentHistory() {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                android.util.Log.d("ProgressViewModel", "Loading history for user: $userId")
                val subBabs = studentProgressRepository.getAllStudentSubBabProgress(userId)
                android.util.Log.d(
                    "ProgressViewModel",
                    "Found ${subBabs.size} subbab progress records"
                )
                val timeFormat = SimpleDateFormat("d MMMM yyyy '-' HH.mm.ss z", Locale("id"))

                val flatPairs = subBabs
                    .sortedByDescending { it.lastActivityDate.toDate() }
                    .map { sp ->
                        val lesson = studentProgressRepository.getLessonById(sp.lessonId)
                        val subBab = studentProgressRepository.getSubBabById(sp.subBabId)
                        val item = StudyHistoryItem(
                            title = lesson?.title.orEmpty(),
                            subtitle = subBab?.title.orEmpty(),
                            timeText = timeFormat.format(sp.lastActivityDate.toDate()),
                            schoolLevel = lesson?.schoolLevel,
                            subjectId = lesson?.idSubject,
                            coverImage = subBab?.coverImage.orEmpty()
                        )
                        val date = sp.lastActivityDate
                            .toDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        date to item
                    }

                val weekFields = WeekFields.ISO
                val groups = flatPairs
                    .groupBy { (date, _) ->
                        val weekStart = date.with(weekFields.dayOfWeek(), 1)
                        val weekEnd = date.with(weekFields.dayOfWeek(), 7)
                        weekStart to weekEnd
                    }
                    .toSortedMap(compareByDescending { it.first })
                    .map { (range, pairs) ->
                        val (start, end) = range
                        val title = formatWeekRange(start, end)
                        WeekGroup(title = title, items = pairs.map { it.second })
                    }
                    .filter { it.items.isNotEmpty() }

                android.util.Log.d("ProgressViewModel", "Created ${groups.size} week groups")
                _history.value = groups

                val dates = flatPairs.map { it.first }.toSet()
                android.util.Log.d(
                    "ProgressViewModel",
                    "Activity dates: ${dates.size} unique dates"
                )
                _activityDates.value = dates
            } catch (e: Exception) {
                android.util.Log.e("ProgressViewModel", "Error loading history", e)
                _history.value = emptyList()
                _activityDates.value = emptySet()
            }
        }
    }

    fun applyClassFilter(level: String?) {
        val filterBy = when (level) {
            "sd" -> ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES
            "smp" -> ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS
            "sma" -> ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS
            else -> ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS
        }
        val options = ProgressClassFilterDialog.ClassFilterOptions(filterBy)
        _sortFilterClassOptions.value = options

        viewModelScope.launch {
            try {
                if (level.isNullOrEmpty()) {
                    _subjectsForLevel.value = emptyList()
                    _selectedSubjects.value = emptySet()
                } else {
                    val subjects = subjectRepository.getSubjectsBySchoolLevel(level)
                    _subjectsForLevel.value = subjects
                    _selectedSubjects.value = emptySet()
                }
            } catch (_: Exception) {
                _subjectsForLevel.value = emptyList()
                _selectedSubjects.value = emptySet()
            }
        }
    }

    fun getProgressFilterOptions(): List<String> {
        return listOf(
            resourceProvider.getString(R.string.learning_history),
            resourceProvider.getString(R.string.title_progress_screen),
            resourceProvider.getString(R.string.done)
        )
    }

    fun getSubjectFilterOptions(): List<String> {
        return listOf(
            resourceProvider.getString(R.string.all_subject),
            resourceProvider.getString(R.string.indonesianLanguage),
            resourceProvider.getString(R.string.math),
            resourceProvider.getString(R.string.naturalScience),
            resourceProvider.getString(R.string.naturalAndSocialScience)
        )
    }

    fun setLearnFilter(filter: LearnFilter) {
        _learnFilter.value = filter
    }

    fun setSelectedSubjects(subjects: Set<String>) {
        _selectedSubjects.value = subjects
    }

    private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
        val monthNames = context.resources.getStringArray(R.array.months_id)
        val startText = "${start.dayOfMonth} ${monthNames[start.monthValue - 1]} ${start.year}"
        val endText = "${end.dayOfMonth} ${monthNames[end.monthValue - 1]} ${end.year}"
        return "$startText - $endText"
    }

    enum class LearnFilter {
        HISTORY, PROGRESS, COMPLETED
    }

    sealed class UiState {
        data object Loading : UiState()
        data object TodayEmpty : UiState()
        data object TodayHasActivity : UiState()
    }
}