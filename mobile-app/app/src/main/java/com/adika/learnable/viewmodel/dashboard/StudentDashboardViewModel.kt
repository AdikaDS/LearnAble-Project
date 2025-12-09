package com.adika.learnable.viewmodel.dashboard

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.Subject
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit
import com.adika.learnable.R
import com.adika.learnable.util.ResourceProvider

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subjectRepository: SubjectRepository,
    private val resourceProvider: ResourceProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _subjectsState = MutableLiveData<SubjectState>()
    val subjectsState: LiveData<SubjectState> = _subjectsState

    private val _selectedSchoolLevel = MutableLiveData<String>()
    val selectedSchoolLevel: LiveData<String> = _selectedSchoolLevel

    private val sharedPreferences =
        context.getSharedPreferences("student_dashboard", Context.MODE_PRIVATE)

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

        sharedPreferences.edit() { putString("selected_school_level", schoolLevel) }

        viewModelScope.launch {
            _subjectsState.value = SubjectState.Loading
            try {
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subjects = subjectRepository.getSubjectsBySchoolLevel(
                        schoolLevel = schoolLevel
                    )
                    _subjectsState.value = SubjectState.Success(subjects)

                } else {
                    _subjectsState.value =
                        SubjectState.Error(resourceProvider.getString(R.string.load_user_data_not_completed))
                }
            } catch (e: Exception) {
                _subjectsState.value = SubjectState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_subjects)
                )
            }
        }
    }

    fun reloadLastSelectedLevel() {
        _selectedSchoolLevel.value?.let { level ->
            loadSubjectsBySchoolLevel(level)
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

} 