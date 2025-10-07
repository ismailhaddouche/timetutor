package com.haddouche.timetutor.model

data class Notificacion(
    val id: String = "",
    val uidDestino: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val fecha: Long = 0L,
    val leida: Boolean = false
)
