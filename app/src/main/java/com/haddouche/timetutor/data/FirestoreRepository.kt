package com.haddouche.timetutor.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.haddouche.timetutor.model.Notification
import java.util.Date

// Centralizo aquí todo el acceso a Firestore para no tener getInstance() por todos lados
// También me sirve para poder hacer tests más fácilmente en el futuro
object FirestoreRepository {
    private const val TAG = "FirestoreRepository"
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun currentUid(): String = auth.currentUser?.uid ?: ""

    // Guardo la notificación en Firestore con callbacks obligatorios
    // Ya no son opcionales para forzar el manejo de errores
    fun sendNotification(
        notification: Notification,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        if (notification.id.isBlank()) {
            Log.w(TAG, "sendNotification: ID de notificación vacío")
            onFailure(RepositoryError.ValidationError("ID de notificación vacío"))
            return
        }

        db.collection("notificaciones")
            .document(notification.id)
            .set(notification)
            .addOnSuccessListener {
                Log.d(TAG, "Notificación enviada: ${notification.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error enviando notificación: ${notification.id}", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Versión fire-and-forget para cuando no necesitamos el resultado
    // Pero igual logueamos errores para debugging
    fun sendNotificationFireAndForget(notification: Notification) {
        if (notification.id.isBlank()) {
            Log.w(TAG, "sendNotificationFireAndForget: ID vacío, ignorando")
            return
        }

        db.collection("notificaciones")
            .document(notification.id)
            .set(notification)
            .addOnSuccessListener {
                Log.d(TAG, "Notificación enviada (fire-and-forget): ${notification.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error enviando notificación (fire-and-forget): ${notification.id}", e)
            }
    }

    // Para operaciones atómicas donde necesito escribir varios documentos a la vez
    fun newBatch(): WriteBatch = db.batch()

    fun getCollectionRef(name: String) = db.collection(name)

    // Query para buscar clases de un alumno con un profesor específico a partir de una fecha
    fun queryLessonsForTeacherStudent(teacherUid: String, studentUid: String, fromDate: String): Query {
        return db.collection("clases")
            .whereEqualTo("teacherUid", teacherUid)
            .whereEqualTo("studentUid", studentUid)
            .whereGreaterThanOrEqualTo("date", fromDate)
    }

    // --- CRUD de clases ---

    // Creo una clase nueva en Firestore
    fun addLesson(
        lessonData: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        db.collection("clases").add(lessonData)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "Clase creada: ${docRef.id}")
                onSuccess(docRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creando clase", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Actualizo el estado de una clase (no_impartida, impartida, ausencia)
    fun updateLessonStatus(
        lessonId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        if (lessonId.isBlank()) {
            onFailure(RepositoryError.ValidationError("ID de clase vacío"))
            return
        }
        db.collection("clases").document(lessonId)
            .update("status", status)
            .addOnSuccessListener {
                Log.d(TAG, "Estado de clase actualizado: $lessonId -> $status")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error actualizando estado de clase: $lessonId", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Elimino una clase
    fun deleteLesson(
        lessonId: String,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        if (lessonId.isBlank()) {
            onFailure(RepositoryError.ValidationError("ID de clase vacío"))
            return
        }
        db.collection("clases").document(lessonId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Clase eliminada: $lessonId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error eliminando clase: $lessonId", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Obtengo las clases de hoy para un profesor
    fun getTodayLessons(
        teacherUid: String,
        todayDate: String,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        db.collection("clases")
            .whereEqualTo("teacherUid", teacherUid)
            .whereEqualTo("date", todayDate)
            .get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error cargando clases de hoy", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Obtengo las clases de la semana para un alumno o profesor
    fun getWeekLessons(
        studentUid: String? = null,
        teacherUid: String? = null,
        fromDate: String,
        toDate: String,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        var query: Query = db.collection("clases")
        
        if (studentUid != null) {
            query = query.whereEqualTo("studentUid", studentUid)
        }
        if (teacherUid != null) {
            query = query.whereEqualTo("teacherUid", teacherUid)
        }
        
        query.whereGreaterThanOrEqualTo("date", fromDate)
            .whereLessThanOrEqualTo("date", toDate)
            .get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.documents)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error cargando clases de la semana", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // --- Códigos de invitación ---

    // Genero un código de invitación para que un alumno se una al profesor
    fun createInvitationCode(
        code: String,
        teacherUid: String,
        expiresAt: Timestamp,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        val data = hashMapOf(
            "code" to code,
            "teacherUid" to teacherUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        )
        db.collection("invitationCodes").document(code).set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Código de invitación creado: $code")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creando código de invitación", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // Valido y canjeo un código de invitación: creo matrícula pendiente y borro el código
    fun redeemInvitationCode(
        code: String,
        studentUid: String,
        onSuccess: (teacherUid: String) -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        val codeRef = db.collection("invitationCodes").document(code)
        codeRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onFailure(RepositoryError.NotFoundError("Código no encontrado o ya usado"))
                    return@addOnSuccessListener
                }

                val teacherUid = doc.getString("teacherUid") ?: ""
                val expiresAt = doc.getTimestamp("expiresAt")

                if (expiresAt != null && expiresAt.toDate().before(Date())) {
                    codeRef.delete()
                    onFailure(RepositoryError.ValidationError("Código expirado"))
                    return@addOnSuccessListener
                }

                if (teacherUid.isBlank()) {
                    onFailure(RepositoryError.ValidationError("Código inválido"))
                    return@addOnSuccessListener
                }

                // Creo matrícula pendiente y borro el código en un batch atómico
                val enrollmentId = "${teacherUid}_${studentUid}"
                val batch = db.batch()

                val enrollmentRef = db.collection("matriculas").document(enrollmentId)
                batch.set(enrollmentRef, mapOf(
                    "teacherUid" to teacherUid,
                    "studentUid" to studentUid,
                    "status" to "pendiente",
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                batch.delete(codeRef)

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Código canjeado y matrícula creada: $enrollmentId")
                        onSuccess(teacherUid)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error canjeando código", e)
                        onFailure(RepositoryError.from(e))
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error leyendo código de invitación", e)
                onFailure(RepositoryError.from(e))
            }
    }

    // --- Perfil de usuario ---

    // Actualizo los campos del perfil sin sobreescribir todo el documento
    fun updateUserProfile(
        uid: String,
        userData: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (RepositoryError) -> Unit
    ) {
        if (uid.isBlank()) {
            onFailure(RepositoryError.ValidationError("UID vacío"))
            return
        }
        db.collection("users").document(uid).update(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Perfil actualizado: $uid")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error actualizando perfil: $uid", e)
                onFailure(RepositoryError.from(e))
            }
    }
}
