package com.haddouche.timetutor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.data.FirestoreRepository
import com.haddouche.timetutor.model.Invoice
import com.haddouche.timetutor.model.Lesson
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.util.NotificationsUtil
import com.haddouche.timetutor.worker.LessonReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// Modelo para un dia de la semana con sus lecciones
data class WeekDay(
    val dayName: String,
    val lessons: List<Lesson>
)

// Modelo para un calendario (profesor) con su estado de matricula
data class StudentCalendar(
    val teacherUid: String,
    val teacher: User,
    val status: String // "pendiente" o "activo"
)

// Modelo para el detalle de un calendario seleccionado
data class StudentCalendarDetail(
    val teacher: User,
    val futureLessons: List<Lesson>,
    val pastLessons: List<Lesson>,
    val invoices: List<Invoice>
)

// Estado de la UI del estudiante
data class StudentHomeUiState(
    val isLoading: Boolean = false,
    val profile: User? = null,
    val todayLessons: List<Lesson> = emptyList(), // Lecciones del día seleccionado
    val weekLessons: List<WeekDay> = emptyList(), // Lecciones de la semana seleccionada
    val calendars: List<StudentCalendar> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val errorMessage: String? = null,
    val joinCodeResult: String? = null,
    val isJoiningWithCode: Boolean = false,
    val selectedCalendarFilter: String? = null,
    val unbilledLessons: List<Lesson> = emptyList(),
    
    // Estado de vista
    val isWeeklyView: Boolean = false,
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedWeekStart: String = "", // Fecha del lunes de la semana seleccionada
    
    // Detalle de calendario seleccionado
    val selectedCalendarDetail: StudentCalendarDetail? = null
)

// ViewModel para manejar la pantalla principal del estudiante
class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    // Guardo todas las lecciones sin filtrar para poder aplicar filtro por calendario
    private var _allWeekLessons: List<WeekDay> = emptyList()

    init {
        loadProfile()
        loadCalendars()
        loadDailyLessons(_uiState.value.selectedDate)
        loadWeekLessons() // Carga la semana actual por defecto
        loadInvoices()
        loadUnbilledLessons()
    }

    // Cargo el perfil del estudiante
    fun loadProfile() {
        if (uid.isEmpty()) return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                _uiState.value = _uiState.value.copy(profile = user)
            }
    }

    // Cargar lecciones del día seleccionado
    fun loadDailyLessons(date: String) {
        if (uid.isEmpty()) return

        // Reutilizamos getWeekLessons pero para un solo día (fromDate == toDate)
        FirestoreRepository.getWeekLessons(
            studentUid = uid,
            teacherUid = null,
            fromDate = date,
            toDate = date,
            onSuccess = { docs ->
                val lessons = docs.map { doc ->
                    mapDocumentToLesson(doc)
                }.sortedBy { it.startTime }
                
                // Aplicar filtro local si existe
                val filtered = if (_uiState.value.selectedCalendarFilter != null) {
                    lessons.filter { it.teacherUid == _uiState.value.selectedCalendarFilter }
                } else {
                    lessons
                }
                
                _uiState.value = _uiState.value.copy(todayLessons = filtered)
                
                // Programar recordatorios locales si es hoy
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (date == today) {
                    scheduleReminders(lessons)
                }
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        )
    }
    
    private fun scheduleReminders(lessons: List<Lesson>) {
        val workManager = WorkManager.getInstance(getApplication())
        val now = Calendar.getInstance()
        
        lessons.forEach { lesson ->
            try {
                val lessonTime = lesson.startTime.split(":")
                val lessonCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, lessonTime[0].toInt())
                    set(Calendar.MINUTE, lessonTime[1].toInt())
                    set(Calendar.SECOND, 0)
                }
                
                // Calcular tiempo hasta 10 min antes de la clase
                val triggerTime = lessonCal.timeInMillis - 10 * 60 * 1000
                val delay = triggerTime - now.timeInMillis
                
                if (delay > 0) {
                    val data = Data.Builder()
                        .putString("lessonTitle", "Clase con ${lesson.teacherName}")
                        .putString("lessonTime", lesson.startTime)
                        .build()
                        
                    val request = OneTimeWorkRequestBuilder<LessonReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag("lesson_${lesson.id}")
                        .build()
                        
                    workManager.enqueue(request)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Cargo las clases de la semana
    fun loadWeekLessons(startDate: String? = null) {
        if (uid.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        val mondayDate: String
        
        if (startDate != null) {
            mondayDate = startDate
            try {
                cal.time = sdf.parse(startDate)!!
            } catch (e: Exception) {
                cal.time = Date()
            }
        } else {
            // Calcular lunes de la semana actual
            val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isoDayOfWeek = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
            val daysToSubtract = isoDayOfWeek - 1
            cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            mondayDate = sdf.format(cal.time)
        }

        _uiState.value = _uiState.value.copy(selectedWeekStart = mondayDate)

        // Generar lista de fechas de la semana para inicializar
        val weekDates = mutableListOf<String>()
        val dayNames = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo")
        
        try {
            cal.time = sdf.parse(mondayDate)!!
            for (i in 0..6) {
                weekDates.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {}

        val sundayDate = weekDates.lastOrNull() ?: mondayDate

        FirestoreRepository.getWeekLessons(
            studentUid = uid,
            teacherUid = null,
            fromDate = mondayDate,
            toDate = sundayDate,
            onSuccess = { docs ->
                val lessons = docs.map { mapDocumentToLesson(it) }
                
                // Agrupar por fecha
                val lessonsByDate = lessons.groupBy { it.date }
                
                // Construir la lista de WeekDay asegurando los 7 días
                val weekDays = weekDates.mapIndexed { index, date ->
                    val dayLessons = lessonsByDate[date]?.sortedBy { it.startTime } ?: emptyList()
                    
                    // Aplicar filtro si existe
                    val filteredLessons = if (_uiState.value.selectedCalendarFilter != null) {
                        dayLessons.filter { it.teacherUid == _uiState.value.selectedCalendarFilter }
                    } else {
                        dayLessons
                    }
                    
                    WeekDay(dayNames[index], filteredLessons)
                }
                
                _uiState.value = _uiState.value.copy(weekLessons = weekDays)
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        )
    }
    
    private fun mapDocumentToLesson(doc: com.google.firebase.firestore.DocumentSnapshot): Lesson {
        return Lesson(
            id = doc.id,
            teacherUid = doc.getString("teacherUid") ?: "",
            teacherName = doc.getString("teacherName") ?: "",
            studentUid = doc.getString("studentUid") ?: "",
            studentName = doc.getString("studentName") ?: "",
            startTime = doc.getString("startTime") ?: "",
            endTime = doc.getString("endTime") ?: "",
            date = doc.getString("date") ?: "",
            status = doc.getString("status") ?: "no_impartida",
            categoryId = doc.getString("categoryId") ?: "",
            categoryName = doc.getString("categoryName") ?: "",
            recurrenceType = doc.getString("recurrenceType") ?: "none",
            isBilled = doc.getBoolean("isBilled") ?: false,
            invoiceId = doc.getString("invoiceId"),
            color = doc.getString("color") ?: "#FFFFFF"
        )
    }

    // Cambio el filtro de calendario
    fun setCalendarFilter(teacherUid: String?) {
        _uiState.value = _uiState.value.copy(selectedCalendarFilter = teacherUid)
        // Recargar vistas con el nuevo filtro
        loadDailyLessons(_uiState.value.selectedDate)
        loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
    }
    
    // Gestión de Vistas y Navegación
    
    fun toggleViewMode() {
        val newMode = !_uiState.value.isWeeklyView
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        _uiState.value = _uiState.value.copy(
            isWeeklyView = newMode,
            selectedDate = if (!newMode) today else _uiState.value.selectedDate
        )
        
        if (!newMode) {
            loadDailyLessons(today)
        } else {
            loadWeekLessons(null) // Reset a semana actual
        }
    }
    
    fun changeDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDailyLessons(date)
    }
    
    fun changeWeek(offset: Int) {
        val currentStart = _uiState.value.selectedWeekStart
        if (currentStart.isBlank()) return
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        try {
            cal.time = sdf.parse(currentStart)!!
            cal.add(Calendar.WEEK_OF_YEAR, offset)
            val newStart = sdf.format(cal.time)
            loadWeekLessons(newStart)
        } catch (e: Exception) {
            loadWeekLessons()
        }
    }

    // Cargo los calendarios de los profesores del estudiante (con estado de matricula)
    fun loadCalendars() {
        if (uid.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        db.collection("matriculas")
            .whereEqualTo("studentUid", uid)
            .get()
            .addOnSuccessListener { enrollmentsSnap ->
                // Guardo el status de cada matricula por teacherUid, excluyendo eliminados
                val statusByTeacher = mutableMapOf<String, String>()
                for (doc in enrollmentsSnap.documents) {
                    val teacherUid = doc.getString("teacherUid") ?: continue
                    val status = doc.getString("status") ?: continue
                    if (status != "eliminado") {
                        statusByTeacher[teacherUid] = status
                    }
                }

                val teacherUids = statusByTeacher.keys.toList()
                if (teacherUids.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calendars = emptyList()
                    )
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), teacherUids)
                    .get()
                    .addOnSuccessListener { usersSnap ->
                        val list = usersSnap.documents.mapNotNull { doc ->
                            val user = doc.toObject(User::class.java)
                            val status = statusByTeacher[doc.id]
                            if (user != null && status != null) {
                                StudentCalendar(
                                    teacherUid = doc.id,
                                    teacher = user,
                                    status = status
                                )
                            } else null
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            calendars = list
                        )
                    }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
    }
    
    // Seleccionar un calendario para ver detalle
    fun selectCalendar(teacherUid: String) {
        if (uid.isEmpty()) return
        
        val calendar = _uiState.value.calendars.find { it.teacherUid == teacherUid } ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Cargar todas las clases de este profesor para este alumno
        db.collection("clases")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("teacherUid", teacherUid)
            .get()
            .addOnSuccessListener { lessonsSnap ->
                val allLessons = lessonsSnap.documents.map { mapDocumentToLesson(it) }
                
                val futureLessons = allLessons.filter { it.date >= today }.sortedBy { it.date + it.startTime }
                val pastLessons = allLessons.filter { it.date < today }.sortedByDescending { it.date + it.startTime }
                
                // Cargar facturas de este profesor
                db.collection("facturas")
                    .whereEqualTo("studentUid", uid)
                    .whereEqualTo("teacherUid", teacherUid)
                    .get()
                    .addOnSuccessListener { invoicesSnap ->
                        val invoices = invoicesSnap.documents.map { doc ->
                            Invoice(
                                id = doc.id,
                                teacherUid = doc.getString("teacherUid") ?: "",
                                studentUid = doc.getString("studentUid") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                date = doc.getString("date") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                paid = doc.getBoolean("paid") ?: false,
                                lessonIds = (doc.get("lessonIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }.sortedByDescending { it.date }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            selectedCalendarDetail = StudentCalendarDetail(
                                teacher = calendar.teacher,
                                futureLessons = futureLessons,
                                pastLessons = pastLessons,
                                invoices = invoices
                            )
                        )
                    }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
    }
    
    fun clearSelectedCalendar() {
        _uiState.value = _uiState.value.copy(selectedCalendarDetail = null)
    }

    // Cargo las facturas del estudiante
    fun loadInvoices() {
        if (uid.isEmpty()) return

        db.collection("facturas")
            .whereEqualTo("studentUid", uid)
            .get()
            .addOnSuccessListener { res ->
                val list = res.documents.map { doc ->
                    Invoice(
                        id = doc.id,
                        teacherUid = doc.getString("teacherUid") ?: "",
                        studentUid = doc.getString("studentUid") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        date = doc.getString("date") ?: "",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        paid = doc.getBoolean("paid") ?: false,
                        lessonIds = (doc.get("lessonIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                _uiState.value = _uiState.value.copy(invoices = list)
            }
    }
    
    // Cargar lecciones pendientes de facturar
    fun loadUnbilledLessons() {
        if (uid.isEmpty()) return
        
        db.collection("clases")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("isBilled", false)
            .whereIn("status", listOf("impartida", "ausencia"))
            .get()
            .addOnSuccessListener { res ->
                val list = res.documents.map { doc ->
                    mapDocumentToLesson(doc)
                }
                _uiState.value = _uiState.value.copy(unbilledLessons = list)
            }
    }
    
    // Generar factura para lecciones pendientes
    fun generateInvoice(teacherUid: String) {
        if (uid.isEmpty()) return
        
        val lessonsToBill = _uiState.value.unbilledLessons.filter { it.teacherUid == teacherUid }
        if (lessonsToBill.isEmpty()) return
        
        val categoryIds = lessonsToBill.map { it.categoryId }.distinct()
        
        db.collection("categorias")
            .whereIn(FieldPath.documentId(), categoryIds)
            .get()
            .addOnSuccessListener { cats ->
                val rates = cats.documents.associate { it.id to (it.getDouble("hourlyRate") ?: 0.0) }
                
                var totalAmount = 0.0
                for (lesson in lessonsToBill) {
                    val rate = rates[lesson.categoryId] ?: 0.0
                    try {
                        val start = lesson.startTime.split(":").map { it.toInt() }
                        val end = lesson.endTime.split(":").map { it.toInt() }
                        val durationHours = (end[0] - start[0]) + (end[1] - start[1]) / 60.0
                        totalAmount += durationHours * rate
                    } catch (e: Exception) {}
                }
                
                val invoiceData = hashMapOf(
                    "teacherUid" to teacherUid,
                    "studentUid" to uid,
                    "studentName" to (_uiState.value.profile?.firstName ?: ""),
                    "date" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "totalAmount" to totalAmount,
                    "paid" to false,
                    "lessonIds" to lessonsToBill.map { it.id }
                )
                
                db.collection("facturas").add(invoiceData)
                    .addOnSuccessListener { docRef ->
                        val batch = db.batch()
                        for (lesson in lessonsToBill) {
                            val ref = db.collection("clases").document(lesson.id)
                            batch.update(ref, mapOf("isBilled" to true, "invoiceId" to docRef.id))
                        }
                        batch.commit().addOnSuccessListener {
                            loadInvoices()
                            loadUnbilledLessons()
                        }
                    }
            }
    }

    // Archivar un calendario (cambiar estado a eliminado)
    fun archiveCalendar(teacherUid: String) {
        if (uid.isEmpty()) return

        db.collection("matriculas")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("teacherUid", teacherUid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    doc.reference.update(
                        mapOf(
                            "status" to "eliminado",
                            "archivedAt" to System.currentTimeMillis()
                        )
                    )
                }
                loadCalendars()
            }
    }

    // --- Codigos de invitacion ---

    fun joinWithCode(code: String) {
        if (uid.isEmpty() || code.isBlank()) return

        _uiState.value = _uiState.value.copy(isJoiningWithCode = true)

        FirestoreRepository.redeemInvitationCode(
            code = code.uppercase().trim(),
            studentUid = uid,
            onSuccess = { teacherUid ->
                _uiState.value = _uiState.value.copy(
                    isJoiningWithCode = false,
                    joinCodeResult = "Te has unido correctamente. Esperando aprobacion del profesor."
                )
                loadCalendars()
                NotificationsUtil.send(
                    teacherUid,
                    "Nuevo alumno pendiente",
                    "Un alumno ha solicitado unirse usando un codigo de invitacion."
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isJoiningWithCode = false,
                    joinCodeResult = "Error: ${error.message}"
                )
            }
        )
    }

    fun clearJoinCodeResult() {
        _uiState.value = _uiState.value.copy(joinCodeResult = null)
    }

    // --- Perfil ---

    fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String,
        showPhone: Boolean,
        showEmail: Boolean,
        email: String,
        tutorWhatsapp: String,
        tutorEmail: String,
        photoUrl: String
    ) {
        if (uid.isEmpty()) return

        val userData = mapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone,
            "showPhone" to showPhone,
            "showEmail" to showEmail,
            "email" to email,
            "tutorWhatsapp" to tutorWhatsapp,
            "tutorEmail" to tutorEmail,
            "photoUrl" to photoUrl
        )

        FirestoreRepository.updateUserProfile(
            uid = uid,
            userData = userData,
            onSuccess = {
                val currentProfile = _uiState.value.profile
                if (currentProfile != null) {
                    _uiState.value = _uiState.value.copy(
                        profile = currentProfile.copy(
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            showPhone = showPhone,
                            showEmail = showEmail,
                            email = email,
                            tutorWhatsapp = tutorWhatsapp,
                            tutorEmail = tutorEmail,
                            photoUrl = photoUrl
                        )
                    )
                }
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
