package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.UserAdminRepository
import com.adika.learnable.util.ResourceProvider
import com.adika.learnable.view.dashboard.admin.dialog.SortFilterDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userAdminRepository: UserAdminRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>()
    val userState: LiveData<UserState> = _userState

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedRoleFilter = MutableLiveData<String>()
    val selectedRoleFilter: LiveData<String> = _selectedRoleFilter

    private val _selectedStatusFilter = MutableLiveData<Boolean?>()
    val selectedStatusFilter: LiveData<Boolean?> = _selectedStatusFilter

    private val _sortFilterOptions = MutableLiveData<SortFilterDialog.SortFilterOptions>()
    val sortFilterOptions: LiveData<SortFilterDialog.SortFilterOptions> = _sortFilterOptions

    private val _allUsers = MutableLiveData<List<User>>()
    val allUsers: LiveData<List<User>> = _allUsers

    init {
        _selectedRoleFilter.value = "all"
        _selectedStatusFilter.value = null
        _sortFilterOptions.value = SortFilterDialog.SortFilterOptions()
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val users = userAdminRepository.getParentAndTeacherUsers()
                _allUsers.value = users
                val sortedUsers = applySorting(users)
                _userState.value = UserState.Success(sortedUsers)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_user_data)
                )
            }
        }
    }

    fun searchUsers(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val users = if (query.isBlank()) {
                    userAdminRepository.getParentAndTeacherUsers()
                } else {
                    userAdminRepository.searchParentAndTeacherUsers(query)
                }
                _allUsers.value = users
                val sortedUsers = applySorting(users)
                _userState.value = UserState.Success(sortedUsers)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_find_user)
                )
            }
        }
    }

    fun filterByRole(role: String) {
        _selectedRoleFilter.value = role
        applyFilters()
    }

    fun filterByStatus(isApproved: Boolean?) {
        _selectedStatusFilter.value = isApproved
        applyFilters()
    }

    private fun applyFilters() {
        val role = _selectedRoleFilter.value ?: "all"
        val status = _selectedStatusFilter.value
        val query = _searchQuery.value ?: ""

        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val users = when {

                    query.isNotBlank() -> {
                        userAdminRepository.searchParentAndTeacherUsers(query)
                    }

                    role != "all" -> {
                        userAdminRepository.getUsersByRole(role)
                    }

                    status != null -> {
                        userAdminRepository.getParentAndTeacherUsersByStatus(status)
                    }

                    else -> {
                        userAdminRepository.getParentAndTeacherUsers()
                    }
                }

                val filteredUsers = users.filter { user ->
                    val roleMatch = role == "all" || user.role == role
                    val statusMatch = status == null || user.isApproved == status
                    roleMatch && statusMatch
                }

                _allUsers.value = filteredUsers
                val sortedUsers = applySorting(filteredUsers)
                _userState.value = UserState.Success(sortedUsers)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_get_user_data)
                )
            }
        }
    }

    fun applySortFilter(options: SortFilterDialog.SortFilterOptions) {
        _sortFilterOptions.value = options
        val currentUsers = _allUsers.value ?: emptyList()
        val sortedUsers = applySorting(currentUsers)
        _userState.value = UserState.Success(sortedUsers)
    }

    private fun applySorting(users: List<User>): List<User> {
        val options = _sortFilterOptions.value ?: SortFilterDialog.SortFilterOptions()

        return when (options.filterBy) {
            SortFilterDialog.SortFilterOptions.FilterBy.DATE -> {
                if (options.sortOrder == SortFilterDialog.SortFilterOptions.SortOrder.ASCENDING) {
                    users.sortedBy { it.createdAt }
                } else {
                    users.sortedByDescending { it.createdAt }
                }
            }

            SortFilterDialog.SortFilterOptions.FilterBy.NAME -> {
                if (options.sortOrder == SortFilterDialog.SortFilterOptions.SortOrder.ASCENDING) {
                    users.sortedBy { it.name.lowercase() }
                } else {
                    users.sortedByDescending { it.name.lowercase() }
                }
            }

            SortFilterDialog.SortFilterOptions.FilterBy.ROLE -> {
                if (options.sortOrder == SortFilterDialog.SortFilterOptions.SortOrder.ASCENDING) {
                    users.sortedBy { it.role ?: "" }
                } else {
                    users.sortedByDescending { it.role ?: "" }
                }
            }

            SortFilterDialog.SortFilterOptions.FilterBy.STATUS -> {
                if (options.sortOrder == SortFilterDialog.SortFilterOptions.SortOrder.ASCENDING) {
                    users.sortedBy { it.isApproved }
                } else {
                    users.sortedByDescending { it.isApproved }
                }
            }
        }
    }

    fun getRoleFilterOptions(): List<String> {
        return listOf(
            resourceProvider.getString(R.string.all_role),
            resourceProvider.getString(R.string.teacher)
        )
    }

    fun getStatusFilterOptions(): List<String> {
        return listOf(
            resourceProvider.getString(R.string.all_status),
            resourceProvider.getString(R.string.approved),
            resourceProvider.getString(R.string.rejected)
        )
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            try {
                val success = userAdminRepository.approveUser(userId)
                if (success) {
                    loadUsers()
                } else {
                    _userState.value = UserState.Error("Gagal menyetujui user")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: "Gagal menyetujui user"
                )
            }
        }
    }

    fun rejectUser(userId: String) {
        viewModelScope.launch {
            try {
                val success = userAdminRepository.rejectUser(userId)
                if (success) {
                    loadUsers()
                } else {
                    _userState.value = UserState.Error("Gagal menolak user")
                }
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: "Gagal menolak user"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.signOut()
                _userState.value = UserState.Success(null)
            } catch (e: Exception) {
                _userState.value =
                    UserState.Error(e.message ?: resourceProvider.getString(R.string.fail_logout))
            }
        }
    }

    sealed class UserState {
        data object Loading : UserState()
        data class Success(val users: List<User>?) : UserState()
        data class Error(val message: String) : UserState()
    }
}