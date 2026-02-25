package com.haddouche.timetutor.ui.alumno

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.haddouche.timetutor.model.Invoice
import com.haddouche.timetutor.model.Lesson
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.ui.common.UserProfileImage
import com.haddouche.timetutor.ui.common.parseColor
import com.haddouche.timetutor.viewmodel.StudentCalendar
import com.haddouche.timetutor.viewmodel.StudentCalendarDetail

@Composable
fun StudentProfileScreen(
    profile: User?,
    invoices: List<Invoice>,
    unbilledLessons: List<Lesson>,
    onSaveProfile: (String, String, String, Boolean, Boolean, String, String, String, String) -> Unit,
    onGenerateInvoice: (String) -> Unit
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var firstName by remember(profile) { mutableStateOf(profile?.firstName ?: "") }
    var lastName by remember(profile) { mutableStateOf(profile?.lastName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var showPhone by remember(profile) { mutableStateOf(profile?.showPhone ?: false) }
    var showEmail by remember(profile) { mutableStateOf(profile?.showEmail ?: false) }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var tutorWhatsapp by remember(profile) { mutableStateOf(profile?.tutorWhatsapp ?: "") }
    var tutorEmail by remember(profile) { mutableStateOf(profile?.tutorEmail ?: "") }
    var photoUrl by remember(profile) { mutableStateOf(profile?.photoUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Sección de Perfil ---
        Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { launcher.launch("image/*") }
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                UserProfileImage(
                    photoUrl = photoUrl,
                    firstName = firstName,
                    lastName = lastName,
                    modifier = Modifier.fillMaxSize(),
                    size = 120.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefono") }, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mostrar telefono")
            Switch(checked = showPhone, onCheckedChange = { showPhone = it })
        }

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mostrar email")
            Switch(checked = showEmail, onCheckedChange = { showEmail = it })
        }

        OutlinedTextField(value = tutorWhatsapp, onValueChange = { tutorWhatsapp = it }, label = { Text("Whatsapp Tutor") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tutorEmail, onValueChange = { tutorEmail = it }, label = { Text("Email Tutor") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (selectedImageUri != null) {
                    val storageRef = FirebaseStorage.getInstance().reference.child("fotos_perfil/$uid.jpg")
                    storageRef.putFile(selectedImageUri!!)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) throw task.exception!!
                            storageRef.downloadUrl
                        }
                        .addOnSuccessListener { uri ->
                            photoUrl = uri.toString()
                            selectedImageUri = null
                            onSaveProfile(firstName, lastName, phone, showPhone, showEmail, email, tutorWhatsapp, tutorEmail, photoUrl)
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    onSaveProfile(firstName, lastName, phone, showPhone, showEmail, email, tutorWhatsapp, tutorEmail, photoUrl)
                    Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Cambios")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- Sección de Facturación ---
        Text("Facturacion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (unbilledLessons.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Clases pendientes de facturar: ${unbilledLessons.size}", fontWeight = FontWeight.Bold)
                    // Agrupar por profesor para generar facturas
                    val byTeacher = unbilledLessons.groupBy { it.teacherUid }
                    byTeacher.forEach { (teacherUid, lessons) ->
                        val teacherName = lessons.firstOrNull()?.teacherName ?: "Profesor"
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$teacherName: ${lessons.size} clases")
                            Button(onClick = { onGenerateInvoice(teacherUid) }) {
                                Text("Generar Factura")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text("Historial de Facturas", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        if (invoices.isEmpty()) {
            Text("No hay facturas", style = MaterialTheme.typography.bodyMedium)
        } else {
            invoices.forEach { invoice ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fecha: ${invoice.date}", fontWeight = FontWeight.Bold)
                            Text("${invoice.totalAmount} \u20ac", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Estado: ${if (invoice.paid) "Pagada" else "Pendiente"}")
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCalendarsContent(
    calendars: List<StudentCalendar>,
    joinCodeResult: String?,
    isJoiningWithCode: Boolean,
    onJoinWithCode: (String) -> Unit,
    onClearJoinResult: () -> Unit,
    onArchiveCalendar: (String) -> Unit,
    onSelectCalendar: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mis Calendarios", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Unirse a calendario
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Unirse a un nuevo calendario", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { 
                            code = it
                            if (joinCodeResult != null) onClearJoinResult()
                        },
                        label = { Text("Codigo de invitacion") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onJoinWithCode(code) },
                        enabled = code.isNotBlank() && !isJoiningWithCode
                    ) {
                        if (isJoiningWithCode) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Unirse")
                        }
                    }
                }
                if (joinCodeResult != null) {
                    Text(
                        text = joinCodeResult,
                        color = if (joinCodeResult.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LaunchedEffect(joinCodeResult) {
                        if (!joinCodeResult.startsWith("Error")) {
                            code = ""
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Lista de calendarios
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(calendars) { calendar ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = calendar.status == "activo") { 
                            onSelectCalendar(calendar.teacherUid) 
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(calendar.teacher.calendarName.ifBlank { "Calendario de ${calendar.teacher.firstName}" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Profesor: ${calendar.teacher.firstName} ${calendar.teacher.lastName}")
                            Text("Estado: ${calendar.status}", style = MaterialTheme.typography.bodySmall, color = if (calendar.status == "activo") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (calendar.status == "activo") {
                            IconButton(onClick = { onArchiveCalendar(calendar.teacherUid) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Archivar/Salir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCalendarDetailScreen(
    detail: StudentCalendarDetail,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = detail.teacher.calendarName.ifBlank { "Calendario de ${detail.teacher.firstName}" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Profesor: ${detail.teacher.firstName} ${detail.teacher.lastName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Agenda") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Historial") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Facturacion") })
        }
        
        // Contenido
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> {
                    if (detail.futureLessons.isEmpty()) {
                        Text("No hay clases programadas", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detail.futureLessons) { lesson ->
                                LessonItem(lesson, showTeacherName = false)
                            }
                        }
                    }
                }
                1 -> {
                    if (detail.pastLessons.isEmpty()) {
                        Text("No hay clases pasadas", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detail.pastLessons) { lesson ->
                                LessonItem(lesson, showTeacherName = false)
                            }
                        }
                    }
                }
                2 -> {
                    if (detail.invoices.isEmpty()) {
                        Text("No hay facturas", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detail.invoices) { invoice ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Fecha: ${invoice.date}", fontWeight = FontWeight.Bold)
                                            Text("${invoice.totalAmount} \u20ac", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text("Estado: ${if (invoice.paid) "Pagada" else "Pendiente"}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonItem(lesson: Lesson, showTeacherName: Boolean) {
    val backgroundColor = parseColor(lesson.color)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lesson.timeSlot,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (showTeacherName && lesson.teacherName.isNotBlank()) {
                    Text(
                        text = lesson.teacherName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (lesson.categoryName.isNotBlank()) {
                Text(
                    text = lesson.categoryName,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Estado de la clase
            val statusText = when(lesson.status) {
                "impartida" -> "Impartida"
                "ausencia" -> "Ausencia"
                else -> "Pendiente"
            }
            val statusColor = when(lesson.status) {
                "impartida" -> MaterialTheme.colorScheme.primary
                "ausencia" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StudentSettingsScreen(
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Configuracion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Tema de la aplicacion", style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = { showThemeDialog = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Cambiar tema")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogOut,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesion")
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Seleccionar tema") },
            text = {
                Column {
                    TextButton(onClick = { onThemeChange("claro"); showThemeDialog = false }) { Text("Claro") }
                    TextButton(onClick = { onThemeChange("oscuro"); showThemeDialog = false }) { Text("Oscuro") }
                    TextButton(onClick = { onThemeChange("sistema"); showThemeDialog = false }) { Text("Seguir sistema") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
