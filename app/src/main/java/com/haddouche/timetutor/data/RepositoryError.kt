package com.haddouche.timetutor.data

import com.google.firebase.FirebaseNetworkException

// Clasificación de errores para una mejor gestión en la UI
// Cada tipo tiene un mensaje amigable para mostrar al usuario
sealed class RepositoryError(val message: String, val cause: Throwable? = null) {

    class NetworkError(message: String = "Sin conexión a Internet", cause: Throwable? = null)
        : RepositoryError(message, cause)

    class AuthError(message: String = "Error de autenticación", cause: Throwable? = null)
        : RepositoryError(message, cause)

    class NotFoundError(message: String = "Datos no encontrados", cause: Throwable? = null)
        : RepositoryError(message, cause)

    class ValidationError(message: String = "Datos inválidos", cause: Throwable? = null)
        : RepositoryError(message, cause)

    class UnknownError(message: String = "Error desconocido", cause: Throwable? = null)
        : RepositoryError(message, cause)

    companion object {
        // Clasifico automáticamente el error según el tipo de excepción
        fun from(exception: Exception): RepositoryError {
            val msg = exception.message ?: "Error desconocido"

            return when {
                // Error de red
                exception is FirebaseNetworkException ->
                    NetworkError("Sin conexión a Internet", exception)

                // Errores de permisos/autenticación
                msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                    AuthError("No tienes permiso para esta operación", exception)
                msg.contains("UNAUTHENTICATED", ignoreCase = true) ->
                    AuthError("Sesión expirada. Vuelve a iniciar sesión", exception)

                // Documento no encontrado
                msg.contains("NOT_FOUND", ignoreCase = true) ->
                    NotFoundError("Datos no encontrados", exception)

                // Datos inválidos
                msg.contains("INVALID_ARGUMENT", ignoreCase = true) ->
                    ValidationError("Datos inválidos", exception)

                // Cualquier otro error
                else -> UnknownError(msg, exception)
            }
        }
    }
}
