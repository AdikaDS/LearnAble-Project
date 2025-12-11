package com.adika.learnable.viewmodel.lesson

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Bookmark
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.BookmarkRepository
import com.adika.learnable.repository.LessonRepository
import com.adika.learnable.repository.MaterialRepository
import com.adika.learnable.repository.QuizRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.repository.SubjectRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val subBabRepository: SubBabRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val subjectRepository: SubjectRepository,
    private val materialRepository: MaterialRepository,
    private val quizRepository: QuizRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _teacherState = MutableLiveData<TeacherState>()
    val teacherState: LiveData<TeacherState> = _teacherState

    private val _subjectProgressState = MutableLiveData<SubjectProgressState>()
    val subjectProgressState: LiveData<SubjectProgressState> = _subjectProgressState

    private val _lessonProgressState = MutableLiveData<LessonProgressState>()
    val lessonProgressState: LiveData<LessonProgressState> = _lessonProgressState

    private val _subBabProgressState = MutableLiveData<SubBabProgressState>()
    val subBabProgressState: LiveData<SubBabProgressState> = _subBabProgressState

    private val _subBabCount = MutableLiveData<Int>()
    val subBabCount: LiveData<Int> = _subBabCount

    private val _lessonCount = MutableLiveData<Int>()
    val lessonCount: LiveData<Int> = _lessonCount

    private val _coverUploadState = MutableLiveData<CoverUploadState>()
    val coverUploadState: LiveData<CoverUploadState> = _coverUploadState

    private val _lesson = MutableLiveData<Lesson?>()
    val lesson: LiveData<Lesson?> = _lesson

    // Bagian untuk Lesson
    fun getLessonsBySubject(idSubject: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val lessons = lessonRepository.getLessonsBySubject(idSubject)
                _studentState.value = StudentState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
                loadSubjectProgress(idSubject)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_lessons)
                )
            }
        }
    }

    fun loadLessonCountBySubject(subjectId: String) {
        viewModelScope.launch {
            try {
                val count = lessonRepository.getLessonCountBySubject(subjectId)
                _lessonCount.value = count
            } catch (e: Exception) {
                _lessonCount.value = 0
            }
        }
    }

    fun getLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                val lesson = lessonRepository.getLesson(lessonId)
                _lesson.value = lesson
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error getting lesson: ${e.message}", e)
                _lesson.value = null
            }
        }
    }

    fun toggleBookmarkForCurrentUser(
        lessonId: String,
        subBab: SubBab,
        subjectId: String?,
        callback: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val studentId = studentProgressRepository.getCurrentUserId()

                val isBookmarked = bookmarkRepository.isBookmarked(studentId, lessonId, subBab.id)

                if (isBookmarked) {

                    val bookmarks = bookmarkRepository.getBookmarksByStudentId(studentId)
                    val bookmarkToRemove = bookmarks.find {
                        it.studentId == studentId &&
                                it.lessonId == lessonId &&
                                it.subBabId == subBab.id
                    }

                    if (bookmarkToRemove != null) {
                        val result = bookmarkRepository.removeBookmark(bookmarkToRemove.id)
                        if (result.isSuccess) {
                            Log.d(
                                "LessonViewModel",
                                "Bookmark removed successfully for subBab: ${subBab.title}"
                            )
                            callback(false) // Bookmark removed, so not bookmarked
                        } else {
                            Log.e(
                                "LessonViewModel",
                                "Failed to remove bookmark: ${result.exceptionOrNull()?.message}"
                            )
                            callback(isBookmarked) // Keep current state on error
                        }
                    } else {
                        callback(false) // Bookmark not found, so not bookmarked
                    }
                } else {

                    val lesson = lessonRepository.getLesson(lessonId)
                    if (lesson == null) {
                        Log.e("LessonViewModel", "Lesson not found for ID: $lessonId")
                        callback(false) // Keep current state on error
                        return@launch
                    }

                    val subject = subjectRepository.getSubjectById(subjectId ?: lesson.idSubject)
                    if (subject == null) {
                        Log.e(
                            "LessonViewModel",
                            "Subject not found for ID: ${subjectId ?: lesson.idSubject}"
                        )
                        callback(false) // Keep current state on error
                        return@launch
                    }

                    val subBabProgress =
                        studentProgressRepository.getStudentSubBabProgress(studentId, subBab.id)
                    val completedMaterials = subBabProgress?.completedMaterials ?: mapOf(
                        "pdf" to false,
                        "video" to false,
                        "quiz" to false
                    )

                    val bookmark = Bookmark(
                        studentId = studentId,
                        subjectId = subjectId ?: lesson.idSubject,
                        lessonId = lessonId,
                        subBabId = subBab.id,
                        completedMaterials = completedMaterials,
                        lessonTitle = lesson.title,
                        subBabTitle = subBab.title,
                        subjectName = subject.name,
                        schoolLevel = lesson.schoolLevel
                    )

                    val result = bookmarkRepository.addBookmark(bookmark)
                    if (result.isSuccess) {
                        Log.d(
                            "LessonViewModel",
                            "Bookmark added successfully for subBab: ${subBab.title}"
                        )
                        callback(true) // Bookmark added successfully
                    } else {
                        Log.e(
                            "LessonViewModel",
                            "Failed to add bookmark: ${result.exceptionOrNull()?.message}"
                        )
                        callback(false) // Keep current state on error
                    }
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error toggling bookmark", e)
                callback(false) // Keep current state on error
            }
        }
    }

    fun checkBookmarkStatusForSubBabs(
        lessonId: String,
        subBabs: List<SubBab>,
        callback: (String, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                for (subBab in subBabs) {
                    val isBookmarked =
                        bookmarkRepository.isBookmarked(studentId, lessonId, subBab.id)
                    callback(subBab.id, isBookmarked)
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error checking bookmark status", e)
            }
        }
    }

    fun searchLessons(query: String, idSubject: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val lessons = lessonRepository.searchLessons(query, idSubject)
                _studentState.value = StudentState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_find_lessons)
                )
            }
        }
    }

    fun searchSubBabs(query: String, idLesson: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val subbab = subBabRepository.searchSubbab(query, idLesson)
                _studentState.value = StudentState.Success(
                    lessons = null,
                    selectedLesson = null,
                    subBabs = subbab
                )
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_find_lessons)
                )
            }
        }
    }

    fun uploadLessonCover(file: java.io.File) {
        viewModelScope.launch {
            _coverUploadState.value = CoverUploadState.Loading
            try {
                val url = lessonRepository.uploadLessonCover(file)
                _coverUploadState.value = CoverUploadState.Success(url)
            } catch (e: IllegalArgumentException) {
                _coverUploadState.value = CoverUploadState.Error(e.message ?: "Gagal upload cover")
            } catch (e: Exception) {
                _coverUploadState.value = CoverUploadState.Error(e.message ?: "Gagal upload cover")
            }
        }
    }

    fun getSubBabsByLesson(lessonId: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val subbab = subBabRepository.getSubBabByLesson(lessonId)
                Log.d("LessonViewModel", "Retrieved ${subbab.size} subbab")
                _studentState.value = StudentState.Success(
                    lessons = null,
                    selectedLesson = null,
                    subBabs = subbab
                )
                loadLessonProgress(lessonId)
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error getting lessons", e)
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_lessons)
                )
            }
        }
    }

    fun loadSubBabCountByLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                val count = subBabRepository.getSubBabCountByLesson(lessonId)
                _subBabCount.value = count
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error loading sub-bab count", e)
                _subBabCount.value = 0
            }
        }
    }

    // Bagian CRUD Lesson
    fun getLessonsByTeacherId(teacherId: String) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val lessons = lessonRepository.getLessonsByTeacherId(teacherId)
                _teacherState.value = TeacherState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
            } catch (e: Exception) {
                _teacherState.value = TeacherState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_lessons)
                )
            }
        }
    }

    fun addLesson(lesson: Lesson, onResult: (Result<Lesson>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val newLesson = lessonRepository.addLesson(lesson)

                lessonRepository.updateLessonTotalSubBab(newLesson.id, 0)
                onResult(Result.success(newLesson))

                lesson.teacherId?.let { getLessonsByTeacherId(it) }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateLesson(lesson: Lesson, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                lessonRepository.updateLesson(lesson)
                onResult(Result.success(Unit))

                lesson.teacherId?.let { getLessonsByTeacherId(it) }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteLesson(lessonId: String, teacherId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {

                val subBabs = subBabRepository.getSubBabByLesson(lessonId)
                val subBabIds = subBabs.map { it.id }

                subBabs.forEach { subBab ->
                    runCatching {
                        materialRepository.deleteAllMaterialsForSubBab(subBab.id)
                    }
                }

                if (subBabIds.isNotEmpty()) {
                    runCatching {
                        quizRepository.deleteAllQuizzesForSubBabs(subBabIds)
                    }
                }

                subBabRepository.deleteAllSubBabsForLesson(lessonId)

                runCatching {
                    lessonRepository.deleteLessonCover(lessonId)
                }

                lessonRepository.updateLessonTotalSubBab(lessonId, 0)

                lessonRepository.deleteLesson(lessonId)

                onResult(Result.success(Unit))

                getLessonsByTeacherId(teacherId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun addSubBab(subBab: SubBab, onResult: (Result<SubBab>) -> Unit) {
        viewModelScope.launch {
            try {
                val newSubBab = subBabRepository.addSubBab(subBab)

                updateTotalSubBabForLesson(subBab.lessonId)
                onResult(Result.success(newSubBab))

                loadSubBabsForLessonTeacher(subBab.lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateSubBab(subBab: SubBab, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                subBabRepository.updateSubBab(subBab)
                onResult(Result.success(Unit))

                loadSubBabsForLessonTeacher(subBab.lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteSubBab(subBabId: String, lessonId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {

                runCatching {
                    materialRepository.deleteAllMaterialsForSubBab(subBabId)
                }

                runCatching {
                    quizRepository.deleteQuizBySubBabId(subBabId)
                }

                subBabRepository.deleteSubBab(subBabId)

                updateTotalSubBabForLesson(lessonId)

                onResult(Result.success(Unit))

                loadSubBabsForLessonTeacher(lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun loadSubBabsForLessonTeacher(lessonId: String) {
        viewModelScope.launch {
            try {
                val currentState = _teacherState.value
                if (currentState is TeacherState.Success) {
                    val lessons = currentState.lessons
                    val selectedLesson = lessons.find { it.id == lessonId }

                    if (selectedLesson != null) {
                        val subBabs = subBabRepository.getSubBabByLesson(lessonId)
                        _teacherState.value = currentState.copy(
                            selectedLesson = selectedLesson,
                            subBabs = subBabs
                        )
                    } else {
                        Log.e("LessonViewModel", "Selected lesson not found: $lessonId")
                    }
                } else {
                    Log.e("LessonViewModel", "Invalid state for loading sub-babs: $currentState")
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error loading sub-babs", e)
                _teacherState.value = TeacherState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_subbab)
                )
            }
        }
    }

    private fun loadSubjectProgress(subjectId: String) {
        val studentId = studentProgressRepository.getCurrentUserId()
        viewModelScope.launch {
            _subjectProgressState.value = SubjectProgressState.Loading
            try {
                val progress = studentProgressRepository.getStudentSubjectProgressBySubjectId(
                    studentId,
                    subjectId
                )
                _subjectProgressState.value = SubjectProgressState.Success(progress)
            } catch (e: Exception) {
                _subjectProgressState.value = SubjectProgressState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_subject_progress)
                )
            }
        }
    }

    private fun loadLessonProgress(lessonId: String) {
        val studentId = studentProgressRepository.getCurrentUserId()
        viewModelScope.launch {
            _lessonProgressState.value = LessonProgressState.Loading
            try {
                val progress =
                    studentProgressRepository.getStudentLessonProgress(studentId, lessonId)
                _lessonProgressState.value = LessonProgressState.Success(progress)
            } catch (e: Exception) {
                _lessonProgressState.value =
                    LessonProgressState.Error(e.message ?: "Fail Load progress")
            }
        }
    }

    fun loadLessonsProgress(lessonIds: List<String>) {
        val studentId = studentProgressRepository.getCurrentUserId()
        viewModelScope.launch {
            lessonIds.forEach { id ->
                try {
                    val progress = studentProgressRepository.getStudentLessonProgress(studentId, id)
                    _lessonProgressState.value = LessonProgressState.Success(progress)
                } catch (e: Exception) {
                    _lessonProgressState.value =
                        LessonProgressState.Error(e.message ?: "Fail Load progress")
                }
            }
        }
    }

    fun loadSubBabProgress(subBabIds: List<String>) {
        val studentId = studentProgressRepository.getCurrentUserId()
        viewModelScope.launch {
            try {
                subBabIds.forEach { id ->
                    try {
                        val progress =
                            studentProgressRepository.getStudentSubBabProgress(studentId, id)
                        _subBabProgressState.value = SubBabProgressState.Success(progress)
                    } catch (e: Exception) {
                        _subBabProgressState.value =
                            SubBabProgressState.Error(e.message ?: "Fail Load progress")
                    }
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error loading sub-bab progress", e)
            }
        }
    }

    private suspend fun updateTotalSubBabForLesson(lessonId: String) {
        try {
            val totalSubBabs = subBabRepository.syncTotalSubBabForLesson(lessonId)
            lessonRepository.updateLessonTotalSubBab(lessonId, totalSubBabs)
            Log.d("LessonViewModel", "Updated totalSubBab for lesson $lessonId to $totalSubBabs")
        } catch (e: Exception) {
            Log.e("LessonViewModel", "Error updating totalSubBab for lesson $lessonId", e)
        }
    }

    fun syncTotalSubBabForLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                updateTotalSubBabForLesson(lessonId)
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error syncing totalSubBab for lesson $lessonId", e)
            }
        }
    }


    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(
            val lessons: List<Lesson>?,
            val selectedLesson: Lesson?,
            val subBabs: List<SubBab>
        ) : StudentState()

        data class Error(val message: String) : StudentState()
    }

    sealed class TeacherState {
        data object Loading : TeacherState()
        data class Success(
            val lessons: List<Lesson>,
            val selectedLesson: Lesson? = null,
            val subBabs: List<SubBab> = emptyList()
        ) : TeacherState()

        data class Error(val message: String) : TeacherState()
    }

    sealed class LessonProgressState {
        data object Loading : LessonProgressState()
        data class Success(val progress: StudentLessonProgress?) : LessonProgressState()
        data class Error(val message: String) : LessonProgressState()
    }

    sealed class SubjectProgressState {
        data object Loading : SubjectProgressState()
        data class Success(val progress: StudentSubjectProgress?) : SubjectProgressState()
        data class Error(val message: String) : SubjectProgressState()
    }

    sealed class SubBabProgressState {
        data object Loading : SubBabProgressState()
        data class Success(val progress: StudentSubBabProgress?) : SubBabProgressState()
        data class Error(val message: String) : SubBabProgressState()
    }

    sealed class CoverUploadState {
        data object Loading : CoverUploadState()
        data class Success(val url: String) : CoverUploadState()
        data class Error(val message: String) : CoverUploadState()
    }
} 