package com.haddouche.timetutor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Timestamp
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

// Modelo para una categoria de precio
data class Category(
    val id: String = "",
    val name: String = "",
    val hourlyRate: Double = 0.0
)

// Estado de la UI del profesor
data class TeacherHomeUiState(
    val isLoading: Boolean = false,
    val profile: User? = null,
    val todayLessons: List<Lesson> = emptyList(),
    val weekLessons: Map<String, List<Lesson>> = emptyMap(), // Mapa de fecha (yyyy-MM-dd) a lista de lecciones
    val activeStudents: List<User> = emptyList(),
    val pendingStudents: List<User> = emptyList(),
    val removedStudents: List<User> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val generatedCode: String? = null,
    val isGeneratingCode: Boolean = false,
    
    // Estado para detalle de alumno
    val selectedStudent: User? = null,
    val studentLessons: List<Lesson> = emptyList(),
    val studentInvoices: List<Invoice> = emptyList(),
    val selectedLessonsForInvoice: Set<String> = emptySet(),

    // Estado para vista de calendario
    val isWeeklyView: Boolean = false,
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedWeekStart: String = "" // Fecha del lunes de la semana seleccionada (yyyy-MM-dd)
)

// ViewModel para manejar la pantalla principal del profesor
class TeacherHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadDailyLessons(_uiState.value.selectedDate)
        loadWeekLessons() // Carga la semana actual por defecto
        loadCategories()
        loadStudents()
        loadInvoices()
    }

    // Cargo el perfil del profesor
    fun loadProfile() {
        if (uid.isEmpty()) return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java)
                _uiState.value = _uiState.value.copy(profile = user)
            }
    }

    // Cargo las clases de un dia especifico desde Firestore
    fun loadDailyLessons(date: String) {
        if (uid.isEmpty()) return

        FirestoreRepository.getTodayLessons(
            teacherUid = uid,
            todayDate = date,
            onSuccess = { docs ->
                val lessons = docs.map { doc ->
                    mapDocumentToLesson(doc)
                }.sortedBy { it.startTime }
                _uiState.value = _uiState.value.copy(todayLessons = lessons)
                
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
                        .putString("lessonTitle", "Clase con ${lesson.studentName}")
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

    // Cargo las clases de la semana. Si startDate es null, calcula la semana actual.
    fun loadWeekLessons(startDate: String? = null) {
        if (uid.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        val mondayDate: String
        
        if (startDate != null) {
            // Usar la fecha proporcionada como inicio (Lunes)
            mondayDate = startDate
            try {
                cal.time = sdf.parse(startDate)!!
            } catch (e: Exception) {
                // Fallback a hoy si falla el parseo
                cal.time = Date()
            }
        } else {
            // Algoritmo robusto para encontrar el lunes de la semana actual
            val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isoDayOfWeek = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
            val daysToSubtract = isoDayOfWeek - 1
            cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
            mondayDate = sdf.format(cal.time)
        }
        
        // Guardamos el inicio de la semana en el estado
        _uiState.value = _uiState.value.copy(selectedWeekStart = mondayDate)
        
        // Generar lista de fechas de la semana (Lunes a Domingo) para inicializar el mapa
        val weekDates = mutableListOf<String>()
        try {
            cal.time = sdf.parse(mondayDate)!!
            for (i in 0..6) {
                weekDates.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {}
        
        // Calcular domingo de esta semana (para la query)
        val sundayDate = weekDates.lastOrNull() ?: mondayDate

        FirestoreRepository.getWeekLessons(
            studentUid = null, 
            teacherUid = uid,
            fromDate = mondayDate,
            toDate = sundayDate,
            onSuccess = { docs ->
                val lessons = docs.map { doc ->
                    mapDocumentToLesson(doc)
                }
                
                // Agrupar lecciones por fecha
                val lessonsByDate = lessons.groupBy { it.date }
                
                // Crear mapa final asegurando que todos los dias de la semana esten presentes
                val fullWeekMap = weekDates.associateWith { date ->
                    lessonsByDate[date]?.sortedBy { it.startTime } ?: emptyList()
                }
                
                _uiState.value = _uiState.value.copy(weekLessons = fullWeekMap)
            },
            onFailure = { error ->
                println("Error loading week lessons: ${error.message}")
                // Si falla la query (ej: falta indice), al menos mostramos los dias vacios
                val emptyMap = weekDates.associateWith { emptyList<Lesson>() }
                _uiState.value = _uiState.value.copy(
                    weekLessons = emptyMap,
                    errorMessage = "Error cargando semana: ${error.message}"
                )
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
            recurrenceEndDate = doc.getString("recurrenceEndDate"),
            recurrenceDays = (doc.get("recurrenceDays") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
            isBilled = doc.getBoolean("isBilled") ?: false,
            invoiceId = doc.getString("invoiceId"),
            color = doc.getString("color") ?: "#FFFFFF"
        )
    }
    
    // Cambiar de semana (offset: -1 para anterior, +1 para siguiente)
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
            // Si falla, recargar semana actual
            loadWeekLessons()
        }
    }

    // Cargo los estudiantes agrupados por estado
    fun loadStudents() {
        if (uid.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        db.collection("matriculas")
            .whereEqualTo("teacherUid", uid)
            .get()
            .addOnSuccessListener { enrollments ->
                val studentIds = enrollments.documents.mapNotNull { it.getString("studentUid") }
                if (studentIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeStudents = emptyList(),
                        pendingStudents = emptyList(),
                        removedStudents = emptyList()
                    )
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), studentIds)
                    .get()
                    .addOnSuccessListener { usersSnap ->
                        val usersById = usersSnap.documents.associateBy { it.id }
                        val active = mutableListOf<User>()
                        val pending = mutableListOf<User>()
                        val removed = mutableListOf<User>()

                        for (enrollment in enrollments) {
                            val sid = enrollment.getString("studentUid") ?: continue
                            val status = enrollment.getString("status") ?: "pendiente"
                            val userDoc = usersById[sid] ?: continue
                            val user = userDoc.toObject(User::class.java) ?: continue

                            when (status) {
                                "activo" -> active.add(user)
                                "pendiente" -> pending.add(user)
                                "eliminado" -> removed.add(user)
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeStudents = active,
                            pendingStudents = pending,
                            removedStudents = removed
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

    // Cargo las facturas
    fun loadInvoices() {
        if (uid.isEmpty()) return

        db.collection("facturas")
            .whereEqualTo("teacherUid", uid)
            .get()
            .addOnSuccessListener { res ->
                val list = res.documents.map { doc ->
                    Invoice(
                        id = doc.id,
                        teacherUid = doc.getString("teacherUid") ?: "",
                        studentUid = doc.getString("studentUid") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        studentLastName = doc.getString("studentLastName") ?: "",
                        studentWhatsapp = doc.getString("studentWhatsapp") ?: "",
                        studentEmail = doc.getString("studentEmail") ?: "",
                        date = doc.getString("date") ?: "",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        paid = doc.getBoolean("paid") ?: false,
                        lessonIds = (doc.get("lessonIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                _uiState.value = _uiState.value.copy(invoices = list)
            }
    }

    // Cargo las categorias de precios
    fun loadCategories() {
        if (uid.isEmpty()) return

        db.collection("categorias")
            .whereEqualTo("teacherUid", uid)
            .get()
            .addOnSuccessListener { res ->
                val list = res.documents.map { doc ->
                    Category(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        hourlyRate = doc.getDouble("hourlyRate") ?: 0.0
                    )
                }
                _uiState.value = _uiState.value.copy(categories = list)
            }
    }

    // Helper para generar lista de lecciones
    private fun generateLessons(
        baseLesson: Map<String, Any>,
        date: String,
        recurrenceType: String,
        recurrenceEndDate: String?,
        recurrenceDays: List<Int>
    ): List<Map<String, Any>> {
        val lessons = mutableListOf<Map<String, Any>>()
        
        if (recurrenceType == "none") {
            val newLesson = HashMap(baseLesson)
            newLesson["date"] = date
            lessons.add(newLesson)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDateObj = try { sdf.parse(date) } catch (e: Exception) { null }
            val endDateObj = if (!recurrenceEndDate.isNullOrBlank()) {
                try { sdf.parse(recurrenceEndDate) } catch (e: Exception) { null }
            } else null

            if (startDateObj == null || endDateObj == null || startDateObj.after(endDateObj)) {
                return emptyList()
            }
            
            val cal = Calendar.getInstance()
            cal.time = startDateObj
            
            while (!cal.time.after(endDateObj)) {
                val currentDateStr = sdf.format(cal.time)
                
                var shouldAdd = false
                when (recurrenceType) {
                    "daily" -> shouldAdd = true
                    "weekly" -> shouldAdd = true
                    "biweekly" -> shouldAdd = true
                    "monthly" -> shouldAdd = true
                    "custom" -> {
                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        val isoDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                        if (recurrenceDays.contains(isoDay)) shouldAdd = true
                    }
                }
                
                if (shouldAdd) {
                    val newLesson = HashMap(baseLesson)
                    newLesson["date"] = currentDateStr
                    lessons.add(newLesson)
                }
                
                when (recurrenceType) {
                    "daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "biweekly" -> cal.add(Calendar.WEEK_OF_YEAR, 2)
                    "monthly" -> cal.add(Calendar.MONTH, 1)
                    "custom" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return lessons
    }

    // Creo una clase nueva y la persisto en Firestore (con soporte para recurrencia)
    fun addLesson(
        startTime: String,
        endTime: String,
        date: String,
        studentUid: String,
        studentName: String,
        categoryId: String,
        categoryName: String,
        recurrenceType: String = "none",
        recurrenceEndDate: String? = null,
        recurrenceDays: List<Int> = emptyList(),
        color: String = "#FFFFFF"
    ) {
        if (uid.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        val teacherName = _uiState.value.profile?.firstName ?: ""

        val baseLesson = hashMapOf<String, Any>(
            "teacherUid" to uid,
            "teacherName" to teacherName,
            "studentUid" to studentUid,
            "studentName" to studentName,
            "startTime" to startTime,
            "endTime" to endTime,
            "status" to "no_impartida",
            "categoryId" to categoryId,
            "categoryName" to categoryName,
            "recurrenceType" to recurrenceType,
            "isBilled" to false,
            "color" to color
        )
        if (recurrenceEndDate != null) baseLesson["recurrenceEndDate"] = recurrenceEndDate
        if (recurrenceDays.isNotEmpty()) baseLesson["recurrenceDays"] = recurrenceDays

        val lessonsToCreate = generateLessons(baseLesson, date, recurrenceType, recurrenceEndDate, recurrenceDays)

        if (lessonsToCreate.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "No se generaron clases. Revisa las fechas.")
            return
        }

        var createdCount = 0
        for (lessonData in lessonsToCreate) {
            FirestoreRepository.addLesson(
                lessonData = lessonData,
                onSuccess = { 
                    createdCount++
                    if (createdCount == lessonsToCreate.size) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        loadDailyLessons(_uiState.value.selectedDate)
                        loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
                        // Si hay un alumno seleccionado, recargar sus lecciones
                        if (_uiState.value.selectedStudent != null) {
                            selectStudent(_uiState.value.selectedStudent!!)
                        }
                        
                        // Notificar al alumno
                        NotificationsUtil.send(
                            studentUid,
                            "Nueva clase programada",
                            "El profesor $teacherName ha programado una clase para el $date a las $startTime."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
                }
            )
        }
    }
    
    // Editar una clase existente
    fun updateLesson(
        lessonId: String,
        startTime: String,
        endTime: String,
        date: String,
        studentUid: String,
        studentName: String,
        categoryId: String,
        categoryName: String,
        recurrenceType: String = "none",
        recurrenceEndDate: String? = null,
        recurrenceDays: List<Int> = emptyList(),
        color: String = "#FFFFFF"
    ) {
        if (uid.isEmpty()) return
        _uiState.value = _uiState.value.copy(isLoading = true)

        val updates = mutableMapOf<String, Any>(
            "startTime" to startTime,
            "endTime" to endTime,
            "date" to date,
            "studentUid" to studentUid,
            "studentName" to studentName,
            "categoryId" to categoryId,
            "categoryName" to categoryName,
            "recurrenceType" to recurrenceType,
            "color" to color
        )
        if (recurrenceEndDate != null) updates["recurrenceEndDate"] = recurrenceEndDate
        if (recurrenceDays.isNotEmpty()) updates["recurrenceDays"] = recurrenceDays
        
        // 1. Actualizar la clase actual
        db.collection("clases").document(lessonId).update(updates)
            .addOnSuccessListener {
                // 2. Si hay recurrencia, generar nuevas clases
                if (recurrenceType != "none") {
                    val teacherName = _uiState.value.profile?.firstName ?: ""
                    val baseLesson = hashMapOf<String, Any>(
                        "teacherUid" to uid,
                        "teacherName" to teacherName,
                        "studentUid" to studentUid,
                        "studentName" to studentName,
                        "startTime" to startTime,
                        "endTime" to endTime,
                        "status" to "no_impartida",
                        "categoryId" to categoryId,
                        "categoryName" to categoryName,
                        "recurrenceType" to recurrenceType,
                        "isBilled" to false,
                        "color" to color
                    )
                    if (recurrenceEndDate != null) baseLesson["recurrenceEndDate"] = recurrenceEndDate
                    if (recurrenceDays.isNotEmpty()) baseLesson["recurrenceDays"] = recurrenceDays

                    // Generar lecciones EXCLUYENDO la fecha actual (ya que esa es la que estamos editando)
                    val allLessons = generateLessons(baseLesson, date, recurrenceType, recurrenceEndDate, recurrenceDays)
                    val newLessons = allLessons.filter { it["date"] != date }
                    
                    if (newLessons.isNotEmpty()) {
                        var createdCount = 0
                        for (lessonData in newLessons) {
                            FirestoreRepository.addLesson(
                                lessonData = lessonData,
                                onSuccess = { 
                                    createdCount++
                                    if (createdCount == newLessons.size) {
                                        _uiState.value = _uiState.value.copy(isLoading = false)
                                        loadDailyLessons(_uiState.value.selectedDate)
                                        loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
                                        if (_uiState.value.selectedStudent != null) {
                                            selectStudent(_uiState.value.selectedStudent!!)
                                        }
                                    }
                                },
                                onFailure = { 
                                    // Log error but continue
                                }
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        loadDailyLessons(_uiState.value.selectedDate)
                        loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
                        if (_uiState.value.selectedStudent != null) {
                            selectStudent(_uiState.value.selectedStudent!!)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadDailyLessons(_uiState.value.selectedDate)
                    loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
                    if (_uiState.value.selectedStudent != null) {
                        selectStudent(_uiState.value.selectedStudent!!)
                    }
                }
                
                // Notificar al alumno sobre la actualización
                val teacherName = _uiState.value.profile?.firstName ?: ""
                NotificationsUtil.send(
                    studentUid,
                    "Clase actualizada",
                    "El profesor $teacherName ha modificado una clase para el $date a las $startTime."
                )
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
    }

    // Actualizo el estado de una clase (no_impartida, impartida, ausencia)
    fun updateLessonStatus(lessonId: String, newStatus: String) {
        if (newStatus !in listOf("no_impartida", "impartida", "ausencia")) return

        FirestoreRepository.updateLessonStatus(
            lessonId = lessonId,
            status = newStatus,
            onSuccess = {
                val updated = _uiState.value.todayLessons.map { lesson ->
                    if (lesson.id == lessonId) lesson.copy(status = newStatus) else lesson
                }
                _uiState.value = _uiState.value.copy(todayLessons = updated)
                loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null }) // Tambien actualizar vista semanal
                
                // Actualizar lista de lecciones del alumno si esta seleccionado
                if (_uiState.value.selectedStudent != null) {
                    val updatedStudentLessons = _uiState.value.studentLessons.map { lesson ->
                        if (lesson.id == lessonId) lesson.copy(status = newStatus) else lesson
                    }
                    _uiState.value = _uiState.value.copy(studentLessons = updatedStudentLessons)
                }
                
                // Notificar al alumno
                val lesson = _uiState.value.todayLessons.find { it.id == lessonId }
                if (lesson != null) {
                    val statusMsg = when(newStatus) {
                        "impartida" -> "marcada como impartida"
                        "ausencia" -> "marcada como ausencia"
                        else -> "actualizada"
                    }
                    NotificationsUtil.send(
                        lesson.studentUid,
                        "Estado de clase actualizado",
                        "Tu clase del ${lesson.date} ha sido $statusMsg."
                    )
                }
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        )
    }

    // Elimino una clase de Firestore y de la lista local
    fun deleteLesson(lessonId: String) {
        val lesson = _uiState.value.todayLessons.find { it.id == lessonId }
        
        FirestoreRepository.deleteLesson(
            lessonId = lessonId,
            onSuccess = {
                val updated = _uiState.value.todayLessons.filter { it.id != lessonId }
                _uiState.value = _uiState.value.copy(todayLessons = updated)
                loadWeekLessons(_uiState.value.selectedWeekStart.ifBlank { null })
                
                if (_uiState.value.selectedStudent != null) {
                    val updatedStudentLessons = _uiState.value.studentLessons.filter { it.id != lessonId }
                    _uiState.value = _uiState.value.copy(studentLessons = updatedStudentLessons)
                }
                
                // Notificar al alumno
                if (lesson != null) {
                    val teacherName = _uiState.value.profile?.firstName ?: ""
                    NotificationsUtil.send(
                        lesson.studentUid,
                        "Clase cancelada",
                        "El profesor $teacherName ha cancelado la clase del ${lesson.date}."
                    )
                }
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        )
    }

    // Anyadir una categoria
    fun addCategory(name: String, hourlyRate: Double) {
        if (uid.isEmpty()) return

        val categoryData = hashMapOf(
            "teacherUid" to uid,
            "name" to name,
            "hourlyRate" to hourlyRate
        )

        db.collection("categorias").add(categoryData)
            .addOnSuccessListener { docRef ->
                val newCategory = Category(
                    id = docRef.id,
                    name = name,
                    hourlyRate = hourlyRate
                )
                val updated = _uiState.value.categories + newCategory
                _uiState.value = _uiState.value.copy(categories = updated)
            }
    }

    // Actualizar estado de estudiante
    fun updateStudentStatus(studentUid: String, newStatus: String) {
        if (uid.isEmpty()) return

        db.collection("matriculas")
            .whereEqualTo("teacherUid", uid)
            .whereEqualTo("studentUid", studentUid)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    doc.reference.update("status", newStatus)
                }
                loadStudents()
                
                // Notificar al alumno
                val statusMsg = if (newStatus == "activo") "aceptado" else "actualizado"
                NotificationsUtil.send(
                    studentUid,
                    "Estado de matrícula",
                    "Tu solicitud de matrícula ha sido $statusMsg."
                )
            }
    }

    // --- Códigos de invitación ---

    // Genero un código alfanumérico de 6 caracteres con caducidad de 48 horas
    fun generateInvitationCode() {
        if (uid.isEmpty()) return

        _uiState.value = _uiState.value.copy(isGeneratingCode = true)

        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val code = (1..6).map { chars.random() }.joinToString("")

        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))

        FirestoreRepository.createInvitationCode(
            code = code,
            teacherUid = uid,
            expiresAt = expiresAt,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    generatedCode = code,
                    isGeneratingCode = false
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isGeneratingCode = false,
                    errorMessage = error.message
                )
            }
        )
    }

    fun clearGeneratedCode() {
        _uiState.value = _uiState.value.copy(generatedCode = null)
    }

    // --- Perfil ---

    // Actualizo el perfil del profesor en Firestore
    fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String,
        showPhone: Boolean,
        showEmail: Boolean,
        calendarName: String,
        email: String,
        photoUrl: String
    ) {
        if (uid.isEmpty()) return

        val userData = mapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone,
            "showPhone" to showPhone,
            "showEmail" to showEmail,
            "calendarName" to calendarName,
            "email" to email,
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
                            calendarName = calendarName,
                            email = email,
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

    // Limpio el mensaje de error
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    // --- Gestión de Alumno Seleccionado y Facturación ---

    fun selectStudent(student: User) {
        _uiState.value = _uiState.value.copy(
            selectedStudent = student,
            isLoading = true,
            selectedLessonsForInvoice = emptySet()
        )

        // Cargar lecciones del alumno
        db.collection("clases")
            .whereEqualTo("teacherUid", uid)
            .whereEqualTo("studentUid", student.uid)
            .get()
            .addOnSuccessListener { lessonsSnap ->
                val lessons = lessonsSnap.documents.map { doc ->
                    mapDocumentToLesson(doc)
                }.sortedByDescending { it.date } // Ordenar por fecha descendente

                // Cargar facturas del alumno
                db.collection("facturas")
                    .whereEqualTo("teacherUid", uid)
                    .whereEqualTo("studentUid", student.uid)
                    .get()
                    .addOnSuccessListener { invoicesSnap ->
                        val invoices = invoicesSnap.documents.map { doc ->
                            Invoice(
                                id = doc.id,
                                teacherUid = doc.getString("teacherUid") ?: "",
                                studentUid = doc.getString("studentUid") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                studentLastName = doc.getString("studentLastName") ?: "",
                                studentWhatsapp = doc.getString("studentWhatsapp") ?: "",
                                studentEmail = doc.getString("studentEmail") ?: "",
                                date = doc.getString("date") ?: "",
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                paid = doc.getBoolean("paid") ?: false,
                                lessonIds = (doc.get("lessonIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }.sortedByDescending { it.date }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            studentLessons = lessons,
                            studentInvoices = invoices
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

    fun clearSelectedStudent() {
        _uiState.value = _uiState.value.copy(selectedStudent = null)
    }

    fun toggleLessonSelection(lessonId: String) {
        val currentSelection = _uiState.value.selectedLessonsForInvoice.toMutableSet()
        if (currentSelection.contains(lessonId)) {
            currentSelection.remove(lessonId)
        } else {
            currentSelection.add(lessonId)
        }
        _uiState.value = _uiState.value.copy(selectedLessonsForInvoice = currentSelection)
    }

    fun generateInvoice() {
        val student = _uiState.value.selectedStudent ?: return
        val selectedIds = _uiState.value.selectedLessonsForInvoice
        if (selectedIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        // Calcular total
        val lessonsToBill = _uiState.value.studentLessons.filter { it.id in selectedIds }
        var totalAmount = 0.0
        
        // Necesitamos el precio por hora de cada categoría.
        // Como Lesson solo tiene categoryName, buscamos en la lista de categorías cargadas.
        val categoriesMap = _uiState.value.categories.associateBy { it.id }

        lessonsToBill.forEach { lesson ->
            val category = categoriesMap[lesson.categoryId]
            val hourlyRate = category?.hourlyRate ?: 0.0
            
            // Calcular duración en horas
            try {
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                val start = format.parse(lesson.startTime)
                val end = format.parse(lesson.endTime)
                if (start != null && end != null) {
                    val diff = end.time - start.time
                    val hours = diff.toDouble() / (1000 * 60 * 60)
                    totalAmount += hours * hourlyRate
                }
            } catch (e: Exception) {
                // Ignorar errores de formato por ahora
            }
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val invoiceData = hashMapOf(
            "teacherUid" to uid,
            "studentUid" to student.uid,
            "studentName" to student.firstName,
            "studentLastName" to student.lastName,
            "studentWhatsapp" to student.phone,
            "studentEmail" to student.email,
            "date" to today,
            "totalAmount" to totalAmount,
            "paid" to false,
            "lessonIds" to selectedIds.toList()
        )

        // Crear factura
        db.collection("facturas").add(invoiceData)
            .addOnSuccessListener { docRef ->
                val invoiceId = docRef.id
                
                // Actualizar lecciones como facturadas (batch write sería mejor)
                val batch = db.batch()
                selectedIds.forEach { lessonId ->
                    val lessonRef = db.collection("clases").document(lessonId)
                    batch.update(lessonRef, "isBilled", true)
                    batch.update(lessonRef, "invoiceId", invoiceId)
                }
                
                batch.commit().addOnSuccessListener {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedLessonsForInvoice = emptySet()
                    )
                    // Recargar datos del alumno
                    selectStudent(student)
                    // Recargar facturas generales
                    loadInvoices()
                    
                    // Notificar al alumno
                    val teacherName = _uiState.value.profile?.firstName ?: ""
                    NotificationsUtil.send(
                        student.uid,
                        "Nueva factura",
                        "El profesor $teacherName ha generado una factura por $totalAmount €."
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
    
    // --- Gestión de Vistas (Diaria/Semanal) ---
    
    fun toggleViewMode() {
        val newMode = !_uiState.value.isWeeklyView
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        _uiState.value = _uiState.value.copy(
            isWeeklyView = newMode,
            // Si volvemos a vista diaria, reseteamos a hoy
            selectedDate = if (!newMode) today else _uiState.value.selectedDate
        )
        
        if (!newMode) {
            loadDailyLessons(today)
        } else {
            // Si cambiamos a vista semanal, recargamos la semana actual (null = actual)
            loadWeekLessons(null)
        }
    }
    
    fun changeDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadDailyLessons(date)
    }
}
