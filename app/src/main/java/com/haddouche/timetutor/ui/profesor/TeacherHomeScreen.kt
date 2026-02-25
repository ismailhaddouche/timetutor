package com.haddouche.timetutor.ui.profesor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haddouche.timetutor.model.Lesson
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.navigation.TeacherBottomNavItem
import com.haddouche.timetutor.ui.common.parseColor
import com.haddouche.timetutor.viewmodel.Category
import com.haddouche.timetutor.viewmodel.TeacherHomeUiState
import com.haddouche.timetutor.viewmodel.TeacherHomeViewModel
import java.text.SimpleDateFormat
import java.util.*

val lessonColors = listOf(
    "#FFFFFF", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", 
    "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", 
    "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4", "#FFECB3", 
    "#FFE0B2", "#FFCCBC", "#D7CCC8", "#F5F5F5", "#CFD8DC"
)

@Composable
fun TeacherHomeScreen(
    viewModel: TeacherHomeViewModel,
    onShowNotifications: () -> Unit,
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Si hay un alumno seleccionado, mostramos la pantalla de detalle
    if (uiState.selectedStudent != null) {
        TeacherStudentDetailScreen(
            uiState = uiState,
            onBack = { viewModel.clearSelectedStudent() },
            onToggleLessonSelection = { id -> viewModel.toggleLessonSelection(id) },
            onGenerateInvoice = { viewModel.generateInvoice() }
        )
    } else {
        TeacherMainScreen(
            uiState = uiState,
            onShowNotifications = onShowNotifications,
            onLogOut = onLogOut,
            onThemeChange = onThemeChange,
            onAddLesson = { start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color ->
                viewModel.addLesson(start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color)
            },
            onEditLesson = { id, start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color ->
                viewModel.updateLesson(id, start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color)
            },
            onUpdateLessonStatus = { id, status -> viewModel.updateLessonStatus(id, status) },
            onDeleteLesson = { id -> viewModel.deleteLesson(id) },
            onAddCategory = { name, rate -> viewModel.addCategory(name, rate) },
            onLoadStudents = { viewModel.loadStudents() },
            onLoadInvoices = { viewModel.loadInvoices() },
            onUpdateStudentStatus = { studentUid, status -> viewModel.updateStudentStatus(studentUid, status) },
            onGenerateInvitationCode = { viewModel.generateInvitationCode() },
            onClearGeneratedCode = { viewModel.clearGeneratedCode() },
            onUpdateProfile = { firstName, lastName, phone, showPhone, showEmail, calendarName, email, photoUrl ->
                viewModel.updateProfile(firstName, lastName, phone, showPhone, showEmail, calendarName, email, photoUrl)
            },
            onClearError = { viewModel.clearError() },
            onStudentClick = { student -> viewModel.selectStudent(student) },
            onToggleViewMode = { viewModel.toggleViewMode() },
            onChangeDate = { date -> viewModel.changeDate(date) },
            onWeekChange = { offset -> viewModel.changeWeek(offset) }
        )
    }
}

@Composable
fun TeacherMainScreen(
    uiState: TeacherHomeUiState,
    onShowNotifications: () -> Unit,
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit,
    onAddLesson: (String, String, String, String, String, String, String, String, String?, List<Int>, String) -> Unit,
    onEditLesson: (String, String, String, String, String, String, String, String, String, String?, List<Int>, String) -> Unit,
    onUpdateLessonStatus: (String, String) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onAddCategory: (String, Double) -> Unit,
    onLoadStudents: () -> Unit,
    onLoadInvoices: () -> Unit,
    onUpdateStudentStatus: (String, String) -> Unit,
    onGenerateInvitationCode: () -> Unit,
    onClearGeneratedCode: () -> Unit,
    onUpdateProfile: (String, String, String, Boolean, Boolean, String, String, String) -> Unit,
    onClearError: () -> Unit = {},
    onStudentClick: (User) -> Unit,
    onToggleViewMode: () -> Unit,
    onChangeDate: (String) -> Unit,
    onWeekChange: (Int) -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Cargo datos cuando cambio de tab
    LaunchedEffect(selectedItem) {
        when (selectedItem) {
            2 -> onLoadStudents()
            3 -> onLoadInvoices()
        }
    }

    // Mostrar error si existe
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                TeacherBottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == item.index,
                        onClick = { selectedItem = item.index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            
            when (selectedItem) {
                0 -> TeacherHomeContent(
                    lessons = uiState.todayLessons,
                    weekLessons = uiState.weekLessons,
                    activeStudents = uiState.activeStudents,
                    categories = uiState.categories,
                    isWeeklyView = uiState.isWeeklyView,
                    selectedDate = uiState.selectedDate,
                    selectedWeekStart = uiState.selectedWeekStart,
                    onShowNotifications = onShowNotifications,
                    onAddLesson = onAddLesson,
                    onEditLesson = onEditLesson,
                    onUpdateStatus = onUpdateLessonStatus,
                    onDeleteLesson = onDeleteLesson,
                    onToggleViewMode = onToggleViewMode,
                    onChangeDate = onChangeDate,
                    onWeekChange = onWeekChange
                )
                1 -> TeacherProfileScreen(
                    profile = uiState.profile,
                    onSaveProfile = onUpdateProfile
                )
                2 -> TeacherStudentsContent(
                    activeStudents = uiState.activeStudents,
                    pendingStudents = uiState.pendingStudents,
                    removedStudents = uiState.removedStudents,
                    categories = uiState.categories,
                    generatedCode = uiState.generatedCode,
                    isGeneratingCode = uiState.isGeneratingCode,
                    onAcceptStudent = { student -> onUpdateStudentStatus(student.uid, "activo") },
                    onRemoveStudent = { student -> onUpdateStudentStatus(student.uid, "eliminado") },
                    onRestoreStudent = { student -> onUpdateStudentStatus(student.uid, "activo") },
                    onGenerateCode = onGenerateInvitationCode,
                    onClearCode = onClearGeneratedCode,
                    onAddLessonForStudent = { student, start, end, date, catId, catName ->
                        onAddLesson(start, end, date, student.uid, student.firstName, catId, catName, "none", null, emptyList(), "#FFFFFF")
                    },
                    onStudentClick = onStudentClick
                )
                3 -> TeacherInvoicesContent()
                4 -> TeacherSettingsScreen(
                    categories = uiState.categories,
                    onAddCategory = onAddCategory,
                    onLogOut = onLogOut,
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

@Composable
fun TeacherHomeContent(
    lessons: List<Lesson>,
    weekLessons: Map<String, List<Lesson>>,
    activeStudents: List<User>,
    categories: List<Category>,
    isWeeklyView: Boolean,
    selectedDate: String,
    selectedWeekStart: String,
    onShowNotifications: () -> Unit,
    onAddLesson: (String, String, String, String, String, String, String, String, String?, List<Int>, String) -> Unit,
    onEditLesson: (String, String, String, String, String, String, String, String, String, String?, List<Int>, String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteLesson: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onChangeDate: (String) -> Unit,
    onWeekChange: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var lessonToEdit by remember { mutableStateOf<Lesson?>(null) }
    val context = LocalContext.current

    val showDatePicker = {
        val cal = Calendar.getInstance()
        // Parsear la fecha seleccionada actual
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(selectedDate)
            if (date != null) cal.time = date
        } catch (e: Exception) {
            // Usar hoy si falla
        }
        
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
                onChangeDate(newDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera con controles de vista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWeeklyView) {
                // Selector de semana
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onWeekChange(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
                    }
                    
                    // Formatear rango de semana
                    val weekRangeText = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val displayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                        val start = sdf.parse(selectedWeekStart)
                        val cal = Calendar.getInstance()
                        cal.time = start!!
                        val startStr = displayFormat.format(cal.time)
                        cal.add(Calendar.DAY_OF_YEAR, 6)
                        val endStr = displayFormat.format(cal.time)
                        "$startStr - $endStr"
                    } catch (e: Exception) {
                        "Semana actual"
                    }

                    Text(
                        text = weekRangeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { onWeekChange(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente")
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        // Dia anterior
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            onChangeDate(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
                    }
                    
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showDatePicker() }
                    )
                    
                    IconButton(onClick = { 
                        // Dia siguiente
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate)!!
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            onChangeDate(sdf.format(cal.time))
                        } catch (e: Exception) {}
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
                    }
                }
            }

            IconButton(onClick = onToggleViewMode) {
                Icon(
                    if (isWeeklyView) Icons.AutoMirrored.Filled.List else Icons.Default.DateRange,
                    contentDescription = "Cambiar vista"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anadir clase")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isWeeklyView) {
            WeeklyLessonsView(
                weekLessons = weekLessons,
                onEdit = { lessonToEdit = it },
                onStatusChange = onUpdateStatus,
                onDelete = onDeleteLesson
            )
        } else {
            if (lessons.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay clases para este día",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(lessons) { lesson ->
                        LessonItem(
                            lesson = lesson,
                            onStatusChange = { status -> onUpdateStatus(lesson.id, status) },
                            onEdit = { lessonToEdit = lesson },
                            onDelete = { onDeleteLesson(lesson.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onShowNotifications,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Centro de notificaciones")
        }
    }

    if (showAddDialog) {
        AddLessonDialog(
            activeStudents = activeStudents,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color ->
                onAddLesson(start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color)
                showAddDialog = false
            }
        )
    }

    if (lessonToEdit != null) {
        EditLessonDialog(
            lessonToEdit = lessonToEdit!!,
            activeStudents = activeStudents,
            categories = categories,
            onDismiss = { lessonToEdit = null },
            onConfirm = { start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color ->
                onEditLesson(lessonToEdit!!.id, start, end, date, sUid, sName, catId, catName, recType, recEnd, recDays, color)
                lessonToEdit = null
            }
        )
    }
}

@Composable
fun WeeklyLessonsView(
    weekLessons: Map<String, List<Lesson>>,
    onEdit: (Lesson) -> Unit,
    onStatusChange: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    // Ordenar las fechas
    val sortedDates = weekLessons.keys.sorted()
    
    if (sortedDates.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay clases esta semana")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedDates) { date ->
                val lessons = weekLessons[date] ?: emptyList()
                if (lessons.isNotEmpty()) {
                    Column {
                        // Formatear la fecha para que sea mas legible (ej: "Lunes, 30 oct")
                        val formattedDate = try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val outputFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
                            val dateObj = inputFormat.parse(date)
                            dateObj?.let { outputFormat.format(it).replaceFirstChar { char -> char.uppercase() } } ?: date
                        } catch (e: Exception) {
                            date
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        lessons.forEach { lesson ->
                            LessonItem(
                                lesson = lesson,
                                onStatusChange = { status -> onStatusChange(lesson.id, status) },
                                onEdit = { onEdit(lesson) },
                                onDelete = { onDelete(lesson.id) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItem(
    lesson: Lesson,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val backgroundColor = parseColor(lesson.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = lesson.timeSlot, style = MaterialTheme.typography.titleMedium)
                    Text(text = lesson.studentName, style = MaterialTheme.typography.bodyMedium)
                    if (lesson.categoryName.isNotBlank()) {
                        Text(
                            text = lesson.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tri-estado: Pendiente, Impartida, Ausencia
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = lesson.status == "no_impartida",
                    onClick = { onStatusChange("no_impartida") },
                    label = { Text("Pendiente") }
                )
                FilterChip(
                    selected = lesson.status == "impartida",
                    onClick = { onStatusChange("impartida") },
                    label = { Text("Impartida") }
                )
                FilterChip(
                    selected = lesson.status == "ausencia",
                    onClick = { onStatusChange("ausencia") },
                    label = { Text("Ausencia") }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar clase") },
            text = { Text("Seguro que quieres eliminar esta clase?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonDialog(
    activeStudents: List<User>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (startTime: String, endTime: String, date: String,
                studentUid: String, studentName: String,
                categoryId: String, categoryName: String,
                recurrenceType: String, recurrenceEndDate: String?, recurrenceDays: List<Int>, color: String) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<User?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("#FFFFFF") }
    
    // Recurrence state
    var recurrenceType by remember { mutableStateOf("none") }
    var recurrenceEndDate by remember { mutableStateOf("") }
    var recurrenceDropdownExpanded by remember { mutableStateOf(false) }

    val showTimePicker = { onTimeSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    val showDatePicker = { onDateSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val disabledFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val isFormValid = selectedStudent != null && selectedCategory != null
            && startTime.isNotBlank() && endTime.isNotBlank() && selectedDate.isNotBlank()
            && (recurrenceType == "none" || recurrenceEndDate.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anadir clase") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Dropdown de alumnos
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.firstName} ${it.lastName}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alumno *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        if (activeStudents.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay alumnos activos") },
                                onClick = { studentDropdownExpanded = false },
                                enabled = false
                            )
                        } else {
                            activeStudents.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text("${student.firstName} ${student.lastName}") },
                                    onClick = {
                                        selectedStudent = student
                                        studentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown de categorias
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.name} (${it.hourlyRate} \u20ac/h)" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        if (categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay categorias") },
                                onClick = { categoryDropdownExpanded = false },
                                enabled = false
                            )
                        } else {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.name} (${cat.hourlyRate} \u20ac/h)") },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Fecha *") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker { selectedDate = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hora inicio
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Hora inicio *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { startTime = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hora fin
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    label = { Text("Hora fin *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { endTime = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de color
                Text("Color de la clase", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessonColors) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColor(colorHex))
                                .border(
                                    width = if (selectedColor == colorHex) 2.dp else 0.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Repeticion", style = MaterialTheme.typography.titleSmall)
                
                // Dropdown de recurrencia
                val recurrenceOptions = mapOf(
                    "none" to "No repetir",
                    "daily" to "Diariamente",
                    "weekly" to "Semanalmente",
                    "biweekly" to "Cada 2 semanas",
                    "monthly" to "Mensualmente"
                )
                
                ExposedDropdownMenuBox(
                    expanded = recurrenceDropdownExpanded,
                    onExpandedChange = { recurrenceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = recurrenceOptions[recurrenceType] ?: "No repetir",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repetir") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceDropdownExpanded,
                        onDismissRequest = { recurrenceDropdownExpanded = false }
                    ) {
                        recurrenceOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    recurrenceType = key
                                    recurrenceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (recurrenceType != "none") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recurrenceEndDate,
                        onValueChange = {},
                        label = { Text("Repetir hasta *") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker { recurrenceEndDate = it } },
                        enabled = false,
                        colors = disabledFieldColors
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        startTime, endTime, selectedDate,
                        selectedStudent!!.uid, selectedStudent!!.firstName,
                        selectedCategory!!.id, selectedCategory!!.name,
                        recurrenceType, if (recurrenceType != "none") recurrenceEndDate else null, emptyList(),
                        selectedColor
                    )
                },
                enabled = isFormValid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonDialog(
    lessonToEdit: Lesson,
    activeStudents: List<User>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (startTime: String, endTime: String, date: String,
                studentUid: String, studentName: String,
                categoryId: String, categoryName: String,
                recurrenceType: String, recurrenceEndDate: String?, recurrenceDays: List<Int>, color: String) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(lessonToEdit.startTime) }
    var endTime by remember { mutableStateOf(lessonToEdit.endTime) }
    var selectedDate by remember { mutableStateOf(lessonToEdit.date) }
    
    // Find initial student and category
    val initialStudent = activeStudents.find { it.uid == lessonToEdit.studentUid }
    val initialCategory = categories.find { it.id == lessonToEdit.categoryId }
    
    var selectedStudent by remember { mutableStateOf(initialStudent) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedColor by remember { mutableStateOf(lessonToEdit.color) }
    
    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    
    // Recurrence state (default to existing or none)
    var recurrenceType by remember { mutableStateOf(lessonToEdit.recurrenceType) }
    var recurrenceEndDate by remember { mutableStateOf(lessonToEdit.recurrenceEndDate ?: "") }
    var recurrenceDropdownExpanded by remember { mutableStateOf(false) }

    val showTimePicker = { onTimeSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    val showDatePicker = { onDateSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val disabledFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val isFormValid = selectedStudent != null && selectedCategory != null
            && startTime.isNotBlank() && endTime.isNotBlank() && selectedDate.isNotBlank()
            && (recurrenceType == "none" || recurrenceEndDate.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar clase") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Dropdown de alumnos
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.firstName} ${it.lastName}" } ?: lessonToEdit.studentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alumno *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        if (activeStudents.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay alumnos activos") },
                                onClick = { studentDropdownExpanded = false },
                                enabled = false
                            )
                        } else {
                            activeStudents.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text("${student.firstName} ${student.lastName}") },
                                    onClick = {
                                        selectedStudent = student
                                        studentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown de categorias
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.let { "${it.name} (${it.hourlyRate} \u20ac/h)" } ?: lessonToEdit.categoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        if (categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay categorias") },
                                onClick = { categoryDropdownExpanded = false },
                                enabled = false
                            )
                        } else {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.name} (${cat.hourlyRate} \u20ac/h)") },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = { Text("Fecha *") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker { selectedDate = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hora inicio
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Hora inicio *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { startTime = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hora fin
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    label = { Text("Hora fin *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { endTime = it } },
                    enabled = false,
                    colors = disabledFieldColors
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de color
                Text("Color de la clase", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessonColors) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColor(colorHex))
                                .border(
                                    width = if (selectedColor == colorHex) 2.dp else 0.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Repeticion", style = MaterialTheme.typography.titleSmall)
                
                // Dropdown de recurrencia
                val recurrenceOptions = mapOf(
                    "none" to "No repetir",
                    "daily" to "Diariamente",
                    "weekly" to "Semanalmente",
                    "biweekly" to "Cada 2 semanas",
                    "monthly" to "Mensualmente"
                )
                
                ExposedDropdownMenuBox(
                    expanded = recurrenceDropdownExpanded,
                    onExpandedChange = { recurrenceDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = recurrenceOptions[recurrenceType] ?: "No repetir",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repetir") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceDropdownExpanded,
                        onDismissRequest = { recurrenceDropdownExpanded = false }
                    ) {
                        recurrenceOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    recurrenceType = key
                                    recurrenceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (recurrenceType != "none") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recurrenceEndDate,
                        onValueChange = {},
                        label = { Text("Repetir hasta *") },
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker { recurrenceEndDate = it } },
                        enabled = false,
                        colors = disabledFieldColors
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        startTime, endTime, selectedDate,
                        selectedStudent?.uid ?: lessonToEdit.studentUid, 
                        selectedStudent?.firstName ?: lessonToEdit.studentName,
                        selectedCategory?.id ?: lessonToEdit.categoryId, 
                        selectedCategory?.name ?: lessonToEdit.categoryName,
                        recurrenceType, if (recurrenceType != "none") recurrenceEndDate else null, emptyList(),
                        selectedColor
                    )
                },
                enabled = isFormValid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
