package com.adika.learnable.viewmodel.lesson

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Bookmark
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.BookmarkRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.view.dashboard.student.progress.ProgressClassFilterDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialBookmarkViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val authRepository: AuthRepository,
    private val subBabRepository: SubBabRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialBookmarkUiState())
    val uiState: StateFlow<MaterialBookmarkUiState> = _uiState.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    private val _filteredBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val filteredBookmarks: StateFlow<List<Bookmark>> = _filteredBookmarks.asStateFlow()

    private val _sortFilterClassOptions =
        MutableStateFlow<ProgressClassFilterDialog.ClassFilterOptions?>(null)
    val sortFilterClassOptions: StateFlow<ProgressClassFilterDialog.ClassFilterOptions?> =
        _sortFilterClassOptions.asStateFlow()

    data class SubjectOption(val idSubject: String, val name: String)

    private val _subjectsForLevel = MutableStateFlow<List<SubjectOption>>(emptyList())
    val subjectsForLevel: StateFlow<List<SubjectOption>> = _subjectsForLevel.asStateFlow()

    private val _selectedSubjects = MutableStateFlow<Set<String>>(emptySet())
    val selectedSubjects: StateFlow<Set<String>> = _selectedSubjects.asStateFlow()

    private val _availableGrades = MutableStateFlow<List<String>>(emptyList())

    fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val userId = authRepository.getCurrentUserId()
                val bookmarksList = bookmarkRepository.getBookmarksByStudentId(userId)
                _bookmarks.value = bookmarksList
                _filteredBookmarks.value = bookmarksList

                val grades = bookmarksList.map { it.schoolLevel }.distinct().sorted()
                _availableGrades.value = listOf(context.getString(R.string.all_level)) + grades

                recomputeAvailableSubjects()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEmpty = bookmarksList.isEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun filterByGrade(grade: String?) {
        Log.d("MaterialBookmarkViewModel", "filterByGrade called with grade: $grade")
        val options = when (grade) {
            "sd" -> ProgressClassFilterDialog.ClassFilterOptions(ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES)
            "smp" -> ProgressClassFilterDialog.ClassFilterOptions(ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS)
            "sma" -> ProgressClassFilterDialog.ClassFilterOptions(ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS)
            else -> ProgressClassFilterDialog.ClassFilterOptions(ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS)
        }
        Log.d(
            "MaterialBookmarkViewModel",
            "Setting sortFilterClassOptions to: $options"
        )
        _sortFilterClassOptions.value = options

        _selectedSubjects.value = emptySet()
        Log.d("MaterialBookmarkViewModel", "Reset selectedSubjects to empty")
        recomputeAvailableSubjects()
        applyFilters()
    }

    fun filterBySubject(subject: String) {

        _selectedSubjects.value =
            if (subject == context.getString(R.string.all_subject)) emptySet() else {

                val found = _subjectsForLevel.value.find { it.name == subject }
                if (found != null) setOf(found.idSubject) else emptySet()
            }
        applyFilters()
    }

    private fun applyFilters() {
        val allBookmarks = _bookmarks.value
        var filtered = allBookmarks

        val level: String? = when (_sortFilterClassOptions.value?.filterBy) {
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES -> "sd"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS -> "smp"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS -> "sma"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS -> null
            else -> null
        }
        if (level != null) {
            filtered = filtered.filter { it.schoolLevel == level }
        }

        val selectedSubjectIds = _selectedSubjects.value
        if (selectedSubjectIds.isNotEmpty()) {
            filtered = filtered.filter { it.subjectId in selectedSubjectIds }
        }

        _filteredBookmarks.value = filtered
        _uiState.value = _uiState.value.copy(isEmpty = filtered.isEmpty())
    }

    private fun recomputeAvailableSubjects() {
        val level: String? = when (_sortFilterClassOptions.value?.filterBy) {
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSES -> "sd"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSJHS -> "smp"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.CLASSSHS -> "sma"
            ProgressClassFilterDialog.ClassFilterOptions.FilterBy.ALLCLASS -> null
            else -> null
        }
        val base = _bookmarks.value.let { all ->
            if (level == null) all else all.filter { it.schoolLevel == level }
        }
        val options = base
            .groupBy { it.subjectId }
            .map { (id, list) ->
                val name = list.firstOrNull()?.subjectName ?: id
                SubjectOption(idSubject = id, name = name)
            }
            .sortedBy { it.name.lowercase() }
        _subjectsForLevel.value = options

        val validIds = options.map { it.idSubject }.toSet()
        _selectedSubjects.value = _selectedSubjects.value.filter { it in validIds }.toSet()
    }

    fun setSelectedSubjects(subjectIds: Set<String>) {
        _selectedSubjects.value = subjectIds
        applyFilters()
    }

    suspend fun getSubBabById(subBabId: String): SubBab? {
        return try {
            subBabRepository.getSubBab(subBabId)
        } catch (_: Exception) {
            null
        }
    }

    fun removeBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                bookmarkRepository.removeBookmark(bookmarkId).fold(
                    onSuccess = {

                        val updatedBookmarks = _bookmarks.value.filter { it.id != bookmarkId }
                        _bookmarks.value = updatedBookmarks
                        applyFilters()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to remove bookmark"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to remove bookmark"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MaterialBookmarkUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null
)
