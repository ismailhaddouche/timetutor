package com.haddouche.timetutor.ui.alumno

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haddouche.timetutor.model.Lesson
import com.haddouche.timetutor.navigation.StudentBottomNavItem
import com.haddouche.timetutor.viewmodel.StudentCalendar
import com.haddouche.timetutor.viewmodel.StudentHomeUiState
import com.haddouche.timetutor.viewmodel.StudentHomeViewModel
import com.haddouche.timetutor.viewmodel.WeekDay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudentHomeScreen(
    viewModel: StudentHomeViewModel,
    onShowNotifications: () -> Unit,
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.selectedCalendarDetail != null) {
        StudentCalendarDetailScreen(
            detail = uiState.selectedCalendarDetail!!,
            onBack = { viewModel.clearSelectedCalendar() }
        )
    } else {
        StudentMainScreen(
            uiState = uiState,
            onShowNotifications = onShowNotifications,
            onLogOut = onLogOut,
            onThemeChange = onThemeChange,
            onLoadCalendars = { viewModel.loadCalendars() },
            onLoadInvoices = { viewModel.loadInvoices() },
            onArchiveCalendar = { teacherUid -> viewModel.archiveCalendar(teacherUid) },
            onJoinWithCode = { code -> viewModel.joinWithCode(code) },
            onClearJoinResult = { viewModel.clearJoinCodeResult() },
            onUpdateProfile = { firstName, lastName, phone, showPhone, showEmail, email, tutorWhatsapp, tutorEmail, photoUrl ->
                viewModel.updateProfile(firstName, lastName, phone, showPhone, showEmail, email, tutorWhatsapp, tutorEmail, photoUrl)
            },
            onCalendarFilterChange = { viewModel.setCalendarFilter(it) },
            onGenerateInvoice = { teacherUid -> viewModel.generateInvoice(teacherUid) },
            onToggleViewMode = { viewModel.toggleViewMode() },
            onChangeDate = { date -> viewModel.changeDate(date) },
            onWeekChange = { offset -> viewModel.changeWeek(offset) },
            onSelectCalendar = { teacherUid -> viewModel.selectCalendar(teacherUid) }
        )
    }
}

@Composable
fun StudentMainScreen(
    uiState: StudentHomeUiState,
    onShowNotifications: () -> Unit,
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit,
    onLoadCalendars: () -> Unit,
    onLoadInvoices: () -> Unit,
    onArchiveCalendar: (String) -> Unit,
    onJoinWithCode: (String) -> Unit,
    onClearJoinResult: () -> Unit,
    onUpdateProfile: (String, String, String, Boolean, Boolean, String, String, String, String) -> Unit,
    onCalendarFilterChange: (String?) -> Unit,
    onGenerateInvoice: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onChangeDate: (String) -> Unit,
    onWeekChange: (Int) -> Unit,
    onSelectCalendar: (String) -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(0) }

    // Cargo datos cuando cambio de tab
    LaunchedEffect(selectedItem) {
        when (selectedItem) {
            1 -> onLoadInvoices() // Cargar facturas al entrar al perfil
            2 -> onLoadCalendars()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                StudentBottomNavItem.items.forEach { item ->
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
            when (selectedItem) {
                0 -> StudentHomeContent(
                    todayLessons = uiState.todayLessons,
                    weekLessons = uiState.weekLessons,
                    calendars = uiState.calendars.filter { it.status == "activo" },
                    selectedFilter = uiState.selectedCalendarFilter,
                    isWeeklyView = uiState.isWeeklyView,
                    selectedDate = uiState.selectedDate,
                    selectedWeekStart = uiState.selectedWeekStart,
                    onFilterChange = onCalendarFilterChange,
                    onShowNotifications = onShowNotifications,
                    onToggleViewMode = onToggleViewMode,
                    onChangeDate = onChangeDate,
                    onWeekChange = onWeekChange
                )
                1 -> StudentProfileScreen(
                    profile = uiState.profile,
                    invoices = uiState.invoices,
                    unbilledLessons = uiState.unbilledLessons,
                    onSaveProfile = onUpdateProfile,
                    onGenerateInvoice = onGenerateInvoice
                )
                2 -> StudentCalendarsContent(
                    calendars = uiState.calendars,
                    joinCodeResult = uiState.joinCodeResult,
                    isJoiningWithCode = uiState.isJoiningWithCode,
                    onJoinWithCode = onJoinWithCode,
                    onClearJoinResult = onClearJoinResult,
                    onArchiveCalendar = onArchiveCalendar,
                    onSelectCalendar = onSelectCalendar
                )
                3 -> StudentSettingsScreen(
                    onLogOut = onLogOut,
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeContent(
    todayLessons: List<Lesson>,
    weekLessons: List<WeekDay>,
    calendars: List<StudentCalendar>,
    selectedFilter: String?,
    isWeeklyView: Boolean,
    selectedDate: String,
    selectedWeekStart: String,
    onFilterChange: (String?) -> Unit,
    onShowNotifications: () -> Unit,
    onToggleViewMode: () -> Unit,
    onChangeDate: (String) -> Unit,
    onWeekChange: (Int) -> Unit
) {
    val context = LocalContext.current
    
    val showDatePicker = {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(selectedDate)
            if (date != null) cal.time = date
        } catch (e: Exception) {}
        
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val newDate = String.format("%04d-%02d-%02d", year, month + 1, day)
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
                // Selector de día
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
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

        // Chips de filtro por calendario
        if (calendars.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Todos") }
                )
                calendars.forEach { calendar ->
                    FilterChip(
                        selected = selectedFilter == calendar.teacherUid,
                        onClick = { onFilterChange(calendar.teacherUid) },
                        label = { Text(calendar.teacher.firstName) }
                    )
                }
            }
        }

        if (isWeeklyView) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(weekLessons) { day ->
                    DayItem(day, showTeacherName = selectedFilter == null && calendars.size > 1)
                }
            }
        } else {
            if (todayLessons.isEmpty()) {
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
                    items(todayLessons) { lesson ->
                        LessonItem(lesson, showTeacherName = selectedFilter == null && calendars.size > 1)
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
}

@Composable
fun DayItem(day: WeekDay, showTeacherName: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = day.dayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (day.lessons.isEmpty()) {
                Text(
                    text = "Sin clases",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                day.lessons.forEach { lesson ->
                    LessonItem(lesson, showTeacherName)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
