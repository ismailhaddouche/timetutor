package com.haddouche.timetutor.model

// Modelo de usuario para Firestore

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "alumno", // "profesor" o "alumno"
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val mostrarTelefono: Boolean = false,
    val mostrarEmail: Boolean = false,
    val nombreCalendario: String = "",
    val fotoUrl: String = "", // url de Google o subida por el usuario
    val whatsappTutor: String = "",
    val correoTutor: String = "",
    val categoria: String = "Sin categoría"
)
