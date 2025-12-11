package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.Subject
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.SubjectRepository
import com.adika.learnable.repository.UserTeacherRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userTeacherRepository: UserTeacherRepository,
    private val subjectRepository: SubjectRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _teacherState = MutableLiveData<TeacherState>()
    val teacherState: LiveData<TeacherState> = _teacherState

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _allLessons = MutableLiveData<List<Lesson>>(emptyList())

    private val _filteredLessons = MutableLiveData<List<Lesson>>(emptyList())
    val filteredLessons: LiveData<List<Lesson>> = _filteredLessons

    private val _selectedSchoolLevel = MutableLiveData<String?>(null)
    val selectedSchoolLevel: LiveData<String?> = _selectedSchoolLevel

    private val _selectedSubjectId = MutableLiveData<String?>(null)
    val selectedSubjectId: LiveData<String?> = _selectedSubjectId

    private val _searchQuery = MutableLiveData<String>()

    private val _subjectsForLevel = MutableLiveData<List<Subject>>(emptyList())
    val subjectsForLevel: LiveData<List<Subject>> = _subjectsForLevel

    fun loadUserData() {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val userParentId = authRepository.getUserData(authRepository.getCurrentUserId())
                _teacherState.value = TeacherState.Success(userParentId)
            } catch (e: Exception) {
                _teacherState.value =
                    TeacherState.Error(
                        e.message ?: resourceProvider.getString(R.string.fail_get_user_data)
                    )
            }
        }
    }

    fun loadAllStudentData() {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val allStudentUser = userTeacherRepository.getAllStudent()
                _studentState.value = StudentState.Success(allStudentUser)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_user_data)
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                authRepository.signOut()
                _teacherState.value = TeacherState.Success(null)
            } catch (e: Exception) {
                _teacherState.value = TeacherState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_logout)
                )
            }
        }
    }

    fun setAllLessons(lessons: List<Lesson>) {
        _allLessons.value = lessons
        applyFilters()
    }

    fun applyClassFilter(level: String?) {
        _selectedSchoolLevel.value = level

        viewModelScope.launch {
            try {
                if (level.isNullOrEmpty()) {
                    _subjectsForLevel.value = emptyList()
                    _selectedSubjectId.value = null
                } else {
                    val subjects = subjectRepository.getSubjectsBySchoolLevel(level)
                    _subjectsForLevel.value = subjects
                    _selectedSubjectId.value = null
                }
            } catch (_: Exception) {
                _subjectsForLevel.value = emptyList()
                _selectedSubjectId.value = null
            }
        }
        applyFilters()
    }

    fun setSelectedSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
        applyFilters()
    }

    fun searchLessons(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val lessons = _allLessons.value ?: emptyList()
        val query = _searchQuery.value.orEmpty().trim()
        val level = _selectedSchoolLevel.value
        val subjectId = _selectedSubjectId.value

        var filtered = lessons

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }

        if (level != null) {
            filtered = filtered.filter { it.schoolLevel == level }
        }

        if (subjectId != null) {
            filtered = filtered.filter { it.idSubject == subjectId }
        }

        _filteredLessons.value = filtered
    }

    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val students: List<User>) : StudentState()
        data class Error(val message: String) : StudentState()
    }

    sealed class TeacherState {
        data object Loading : TeacherState()
        data class Success(val teacher: User?) : TeacherState()
        data class Error(val message: String) : TeacherState()
    }
} 