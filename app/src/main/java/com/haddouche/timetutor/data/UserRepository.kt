package com.haddouche.timetutor.data

import android.util.Log
import com.haddouche.timetutor.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot

// Manejo de usuarios en Firestore
object UserRepository {
    private const val TAG = "UserRepository"
    private val usersRef = FirestoreRepository.getCollectionRef("users")

    fun getUser(uid: String): Task<DocumentSnapshot> {
        return usersRef.document(uid).get()
    }

    // Devuelve el rol del usuario (profesor o alumno)
    // onFailure ahora es obligatorio para forzar el manejo de errores
    fun getRole(
        uid: String,
        onSuccess: (String) -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        if (uid.isBlank()) {
            Log.w(TAG, "getRole: UID vacío")
            onFailure(RepositoryError.ValidationError("UID no puede estar vacío"))
            return
        }

        usersRef.document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role")
                    if (role.isNullOrBlank()) {
                        Log.w(TAG, "Usuario $uid no tiene campo role, usando 'alumno' por defecto")
                    }
                    onSuccess(role ?: "alumno")
                } else {
                    Log.w(TAG, "Documento de usuario no encontrado: $uid")
                    onFailure(RepositoryError.NotFoundError("Usuario no encontrado"))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo rol para $uid", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Versión con Result wrapper para código más limpio
    fun getUserResult(
        uid: String,
        onResult: (RepositoryResult<User>) -> Unit
    ) {
        if (uid.isBlank()) {
            onResult(RepositoryResult.Error(RepositoryError.ValidationError("UID vacío")))
            return
        }

        usersRef.document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    try {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            onResult(RepositoryResult.Success(user))
                        } else {
                            Log.e(TAG, "Error parseando documento de usuario: $uid")
                            onResult(RepositoryResult.Error(RepositoryError.ValidationError("Error parseando datos del usuario")))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Excepción parseando usuario: $uid", e)
                        onResult(RepositoryResult.Error(RepositoryError.from(e)))
                    }
                } else {
                    onResult(RepositoryResult.Error(RepositoryError.NotFoundError("Usuario no encontrado")))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo usuario $uid", e)
                onResult(RepositoryResult.Error(RepositoryError.from(e)))
            }
    }
}
