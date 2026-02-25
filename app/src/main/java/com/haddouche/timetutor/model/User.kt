package com.haddouche.timetutor.model

// Modelo del usuario, puede ser profesor o alumno según el campo role
data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "alumno",        // "profesor" o "alumno"
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val showPhone: Boolean = false,     // Si quiere que otros vean su teléfono
    val showEmail: Boolean = false,
    val calendarName: String = "",      // Nombre que muestra el profe en su calendario
    val photoUrl: String = "",          // URL de la foto de perfil
    val tutorWhatsapp: String = "",     // Datos del tutor (para alumnos menores)
    val tutorEmail: String = ""
)
