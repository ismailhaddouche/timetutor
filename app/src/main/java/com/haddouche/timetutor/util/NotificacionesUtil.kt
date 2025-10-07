package com.haddouche.timetutor.util

import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.model.Notificacion

object NotificacionesUtil {
    fun enviarNotificacion(uidDestino: String, titulo: String, mensaje: String) {
        val db = FirebaseFirestore.getInstance()
        val noti = Notificacion(
            id = db.collection("notificaciones").document().id,
            uidDestino = uidDestino,
            titulo = titulo,
            mensaje = mensaje,
            fecha = System.currentTimeMillis(),
            leida = false
        )
        db.collection("notificaciones").document(noti.id).set(noti)
    }
}
