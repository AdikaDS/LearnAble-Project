package com.adika.learnable.viewmodel.progress

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.LessonProgressItem
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.SubBabDoneItem
import com.adika.learnable.model.SubBabProgressItem
import com.adika.learnable.model.SubjectProgressItem
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgressDetailViewModel @Inject constructor(
    private val studentProgressRepository: StudentProgressRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Loading)
    val uiState: LiveData<UiState> = _uiState

    private val _overallProgress = MutableLiveData<List<StudentOverallProgress>>(emptyList())
    val overallProgress: LiveData<List<StudentOverallProgress>> = _overallProgress

    private val _subjectProgress = MutableLiveData<List<SubjectProgressItem>>(emptyList())
    val subjectProgress: LiveData<List<SubjectProgressItem>> = _subjectProgress

    private val _lessonProgress = MutableLiveData<List<LessonProgressItem>>(emptyList())
    val lessonProgress: LiveData<List<LessonProgressItem>> = _lessonProgress

    private val _subBabProgress = MutableLiveData<List<SubBabProgressItem>>(emptyList())
    val subBabProgress: LiveData<List<SubBabProgressItem>> = _subBabProgress

    private val _subBabDoneProgress = MutableLiveData<List<SubBabDoneItem>>(emptyList())
    val subBabDoneProgress: LiveData<List<SubBabDoneItem>> = _subBabDoneProgress

    private fun loadOverallProgress() {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val overall = studentProgressRepository.getStudentOverallProgress(userId)

                if (overall != null) {
                    Log.d(
                        "ProgressDetailVM",
                        "Overall progress loaded: ${overall.overallProgressPercentage}%"
                    )
                    _overallProgress.value = listOf(overall)
                } else {
                    _overallProgress.value = emptyList()
                }
            } catch (e: Exception) {
                _overallProgress.value = emptyList()
            }
        }
    }

    private fun loadSubjectProgress() {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val overall = studentProgressRepository.getStudentOverallProgress(userId)

                if (overall != null && overall.subjectProgress.isNotEmpty()) {
                    Log.d(
                        "ProgressDetailVM",
                        "Found ${overall.subjectProgress.size} subject progress items"
                    )
                    val subjectProgressItems = mutableListOf<SubjectProgressItem>()

                    for (progress in overall.subjectProgress) {
                        try {
                            val subject = subjectRepository.getSubjectsBySchoolLevel("sd")
                                .find { it.idSubject == progress.subjectId }
                                ?: subjectRepository.getSubjectsBySchoolLevel("smp")
                                    .find { it.idSubject == progress.subjectId }
                                ?: subjectRepository.getSubjectsBySchoolLevel("sma")
                                    .find { it.idSubject == progress.subjectId }

                            if (subject != null) {
                                subjectProgressItems.add(SubjectProgressItem(subject, progress))
                            } else {
                                Log.w(
                                    "ProgressDetailVM",
                                    "Subject not found for ID: ${progress.subjectId}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(
                                "ProgressDetailVM",
                                "Error loading subject ${progress.subjectId}",
                                e
                            )
                        }
                    }

                    Log.d(
                        "ProgressDetailVM",
                        "Total subject progress items: ${subjectProgressItems.size}"
                    )
                    _subjectProgress.value = subjectProgressItems
                } else {
                    _subjectProgress.value = emptyList()
                }
            } catch (e: Exception) {
                _subjectProgress.value = emptyList()
            }
        }
    }

    private fun loadLessonProgress(subjectId: String? = null) {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val overall = studentProgressRepository.getStudentOverallProgress(userId)

                if (overall != null) {
                    val lessonProgressItems = mutableListOf<LessonProgressItem>()

                    val subjectProgressList = if (subjectId != null) {
                        overall.subjectProgress.filter { it.subjectId == subjectId }
                    } else {
                        overall.subjectProgress
                    }

                    for (subjectProgress in subjectProgressList) {
                        for (lessonProgress in subjectProgress.lessonProgress) {
                            try {
                                val lesson =
                                    studentProgressRepository.getLessonById(lessonProgress.lessonId)
                                if (lesson != null) {
                                    lessonProgressItems.add(
                                        LessonProgressItem(
                                            lesson,
                                            lessonProgress
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(
                                    "ProgressDetailVM",
                                    "Error loading lesson ${lessonProgress.lessonId}",
                                    e
                                )
                            }
                        }
                    }

                    _lessonProgress.value = lessonProgressItems
                } else {
                    _lessonProgress.value = emptyList()
                }
            } catch (e: Exception) {
                _lessonProgress.value = emptyList()
            }
        }
    }

    private fun loadSubBabProgress(lessonId: String? = null) {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val subBabProgressList =
                    studentProgressRepository.getAllStudentSubBabProgress(userId)

                val subBabProgressItems = mutableListOf<SubBabProgressItem>()

                for (progress in subBabProgressList) {

                    if (lessonId != null && progress.lessonId != lessonId) {
                        continue
                    }

                    try {

                        val subBab = studentProgressRepository.getSubBabById(progress.subBabId)

                        if (subBab != null) {
                            subBabProgressItems.add(SubBabProgressItem(subBab, progress))
                        }
                    } catch (e: Exception) {
                        Log.w("ProgressDetailVM", "Error loading sub-bab ${progress.subBabId}", e)
                    }
                }

                _subBabProgress.value = subBabProgressItems
            } catch (e: Exception) {
                _subBabProgress.value = emptyList()
            }
        }
    }

    private fun loadSubBabDoneProgress(lessonId: String? = null) {
        viewModelScope.launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val subBabProgressList =
                    studentProgressRepository.getAllStudentSubBabProgress(userId)

                val subBabDoneItems = mutableListOf<SubBabDoneItem>()

                for (progress in subBabProgressList) {

                    if (!progress.isCompleted) {
                        continue
                    }

                    if (lessonId != null && progress.lessonId != lessonId) {
                        continue
                    }

                    try {

                        val lesson = studentProgressRepository.getLessonById(progress.lessonId)
                        val subBab = studentProgressRepository.getSubBabById(progress.subBabId)
                        val title = lesson?.title ?: ""
                        val subtitle = subBab?.title ?: ""
                        if (subBab != null) {
                            subBabDoneItems.add(
                                SubBabDoneItem(
                                    title = title,
                                    subtitle = subtitle,
                                    coverImage = subBab.coverImage,
                                    progress = progress
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(
                            "ProgressDetailVM",
                            "Error loading completed sub-bab ${progress.subBabId}",
                            e
                        )
                    }
                }

                subBabDoneItems.sortByDescending { it.progress.lastActivityDate.toDate() }

                _subBabDoneProgress.value = subBabDoneItems
            } catch (e: Exception) {
                _subBabDoneProgress.value = emptyList()
            }
        }
    }

    fun loadAllProgress() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {

                val overallDeferred = async { loadOverallProgress() }
                val subjectDeferred = async { loadSubjectProgress() }
                val lessonDeferred = async { loadLessonProgress() }
                val subBabDeferred = async { loadSubBabProgress() }
                val subBabDoneDeferred = async { loadSubBabDoneProgress() }

                awaitAll(
                    overallDeferred,
                    subjectDeferred,
                    lessonDeferred,
                    subBabDeferred,
                    subBabDoneDeferred
                )

                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
