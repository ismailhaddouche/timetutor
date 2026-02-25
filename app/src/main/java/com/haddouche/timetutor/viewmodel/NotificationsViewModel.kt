package com.haddouche.timetutor.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.haddouche.timetutor.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado de la UI de notificaciones
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val errorMessage: String? = null
)

// ViewModel para manejar las notificaciones
class NotificationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    // Cargo las notificaciones de los ultimos 30 dias
    fun loadNotifications() {
        if (uid.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000

        db.collection("notificaciones")
            .whereEqualTo("targetUid", uid)
            .whereGreaterThanOrEqualTo("timestamp", thirtyDaysAgo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val notifications = result.map { doc ->
                    Notification(
                        id = doc.getString("id") ?: doc.id,
                        targetUid = doc.getString("targetUid") ?: "",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        read = doc.getBoolean("read") ?: false
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    notifications = notifications
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
    }

    // Marco una notificacion como leida
    fun markAsRead(notificationId: String) {
        db.collection("notificaciones").document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                // Actualizo el estado local
                val updated = _uiState.value.notifications.map { notif ->
                    if (notif.id == notificationId) notif.copy(read = true) else notif
                }
                _uiState.value = _uiState.value.copy(notifications = updated)
            }
    }

    // Limpio el mensaje de error
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
