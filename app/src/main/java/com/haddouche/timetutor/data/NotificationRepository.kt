package com.haddouche.timetutor.data

import android.util.Log
import com.haddouche.timetutor.model.Notification
import com.google.firebase.Timestamp
import java.util.Date

// Aquí manejo todo lo relacionado con notificaciones
// Las notificaciones expiran a los 30 días gracias al TTL de Firestore
object NotificationRepository {
    private const val TAG = "NotificationRepository"

    // Envío una notificación con manejo de errores obligatorio
    fun send(
        targetUid: String,
        title: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        // Validaciones
        if (targetUid.isBlank()) {
            Log.w(TAG, "send: targetUid vacío")
            onFailure(RepositoryError.ValidationError("UID destino vacío"))
            return
        }
        if (title.isBlank()) {
            Log.w(TAG, "send: título vacío")
            onFailure(RepositoryError.ValidationError("Título de notificación vacío"))
            return
        }
        if (message.isBlank()) {
            Log.w(TAG, "send: mensaje vacío")
            onFailure(RepositoryError.ValidationError("Mensaje de notificación vacío"))
            return
        }

        val id = FirestoreRepository.getCollectionRef("notificaciones").document().id
        val now = System.currentTimeMillis()
        val ttlMillis = 30L * 24 * 60 * 60 * 1000  // 30 días en milisegundos
        val expiresAtTs = Timestamp(Date(now + ttlMillis))

        val notification = Notification(
            id = id,
            targetUid = targetUid,
            title = title,
            message = message,
            timestamp = now,
            read = false,
            expiresAt = expiresAtTs
        )

        FirestoreRepository.sendNotification(notification, onSuccess, onFailure)
    }

    // Versión fire-and-forget para casos donde no necesitamos el resultado
    // Aún así logueamos errores para debugging
    fun sendFireAndForget(
        targetUid: String,
        title: String,
        message: String
    ) {
        // Validaciones básicas con logging
        if (targetUid.isBlank() || title.isBlank() || message.isBlank()) {
            Log.w(TAG, "sendFireAndForget: parámetros inválidos - targetUid=$targetUid, title=$title")
            return
        }

        val id = FirestoreRepository.getCollectionRef("notificaciones").document().id
        val now = System.currentTimeMillis()
        val ttlMillis = 30L * 24 * 60 * 60 * 1000
        val expiresAtTs = Timestamp(Date(now + ttlMillis))

        val notification = Notification(
            id = id,
            targetUid = targetUid,
            title = title,
            message = message,
            timestamp = now,
            read = false,
            expiresAt = expiresAtTs
        )

        FirestoreRepository.sendNotificationFireAndForget(notification)
    }
}
