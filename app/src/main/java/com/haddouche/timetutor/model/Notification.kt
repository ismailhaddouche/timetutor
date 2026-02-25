package com.haddouche.timetutor.model

import com.google.firebase.Timestamp

// Modelo para las notificaciones push que se guardan en Firestore
data class Notification(
    val id: String = "",
    val targetUid: String = "",       // UID del usuario que recibe la notificación
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,         // Momento en que se creó
    val read: Boolean = false,
    val expiresAt: Timestamp? = null  // TTL de Firestore, las borra automáticamente a los 30 días
)
