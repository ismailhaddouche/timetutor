package com.haddouche.timetutor.util

import com.haddouche.timetutor.data.NotificationRepository

// Helper para enviar notificaciones desde cualquier parte de la app
// Usa fire-and-forget internamente pero logueando errores
object NotificationsUtil {
    fun send(targetUid: String, title: String, message: String) {
        NotificationRepository.sendFireAndForget(targetUid, title, message)
    }
}
