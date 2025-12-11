package com.adika.learnable.viewmodel.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.Notification
import com.adika.learnable.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = notificationRepository.getCurrentUserId()
                val notificationList = notificationRepository.getNotifications(userId)
                _notifications.value = notificationList

                val unread = notificationList.count { !it.isRead }
                _unreadCount.value = unread
            } catch (e: Exception) {
                _notifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)

                loadNotifications()
            } catch (_: Exception) {

            }
        }
    }

    fun getUnreadCount() {
        viewModelScope.launch {
            try {
                val userId = notificationRepository.getCurrentUserId()
                val count = notificationRepository.getUnreadCount(userId)
                _unreadCount.value = count
            } catch (e: Exception) {
                _unreadCount.value = 0
            }
        }
    }
}