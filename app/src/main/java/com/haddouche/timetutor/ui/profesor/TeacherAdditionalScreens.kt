package com.haddouche.timetutor.ui.profesor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.model.Invoice
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.viewmodel.Category
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsContent(
    activeStudents: List<User>,
    pendingStudents: List<User>,
    removedStudents: List<User>,
    categories: List<Category>,
    generatedCode: String?,
    isGeneratingCode: Boolean,
    onAcceptStudent: (User) -> Unit,
    onRemoveStudent: (User) -> Unit,
    onRestoreStudent: (User) -> Unit,
    onGenerateCode: () -> Unit,
    onClearCode: () -> Unit,
    onAddLessonForStudent: (User, String, String, String, String, String) -> Unit,
    onStudentClick: (User) -> Unit // Nuevo callback
) {
    val context = LocalContext.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var showEditLessonDialog by remember { mutableStateOf<User?>(null) }
    // showInfoDialog ya no es necesario para alumnos activos, usamos la nueva pantalla

    Column(modifier = Modifier.fillMaxSize()) {
        // Seccion de codigo de invitacion
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Codigo de invitacion", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (generatedCode != null) {
                    // Abro el share sheet automaticamente al generarse el codigo
                    LaunchedEffect(generatedCode) {
                        val shareText = "Tu codigo de invitacion para TimeTutor es: $generatedCode\nIntroducelo en la app para unirte. Valido 48 horas."
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir codigo"))
                    }

                    Text(
                        text = generatedCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Valido durante 48 horas. Un solo uso.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val shareText = "Tu codigo de invitacion para TimeTutor es: $generatedCode\nIntroducelo en la app para unirte. Valido 48 horas."
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir codigo"))
                        }) { Text("Compartir") }
                        OutlinedButton(onClick = onClearCode) { Text("Cerrar") }
                    }
                } else {
                    Button(
                        onClick = onGenerateCode,
                        enabled = !isGeneratingCode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Generar codigo de invitacion")
                    }
                }
            }
        }

        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Activos") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Pendientes") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Eliminados") })
        }

        val studentsToShow = when (tabIndex) {
            0 -> activeStudents
            1 -> pendingStudents
            2 -> removedStudents
            else -> activeStudents
        }

        if (studentsToShow.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (tabIndex) {
                        0 -> "No hay alumnos activos"
                        1 -> "No hay alumnos pendientes"
                        else -> "No hay alumnos eliminados"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(studentsToShow) { student ->
                    StudentItem(
                        student = student,
                        tabIndex = tabIndex,
                        onAccept = {
                            if (tabIndex == 1) onAcceptStudent(student)
                            else onRestoreStudent(student)
                        },
                        onRemove = { onRemoveStudent(student) },
                        onAssign = { showEditLessonDialog = student },
                        onClick = { 
                            if (tabIndex == 0) onStudentClick(student) 
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showEditLessonDialog != null) {
        EditLessonDialog(
            student = showEditLessonDialog!!,
            categories = categories,
            onDismiss = { showEditLessonDialog = null },
            onSave = { start, end, date, catId, catName ->
                onAddLessonForStudent(showEditLessonDialog!!, start, end, date, catId, catName)
                showEditLessonDialog = null
            }
        )
    }
}

@Composable
fun StudentItem(
    student: User,
    tabIndex: Int,
    onAccept: () -> Unit,
    onRemove: () -> Unit,
    onAssign: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = tabIndex == 0, onClick = onClick), // Clickable solo si es activo
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.firstName, fontWeight = FontWeight.Bold)
                Text(student.calendarName, style = MaterialTheme.typography.bodyMedium)
            }

            Row {
                if (tabIndex == 0) {
                    Button(onClick = onAssign) { Text("Clase") }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Eliminar") }
                } else if (tabIndex == 1) {
                    Button(onClick = onAccept) { Text("Aceptar") }
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Rechazar") }
                } else {
                    Button(onClick = onAccept) { Text("Restaurar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonDialog(
    student: User,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (startTime: String, endTime: String, date: String, categoryId: String, categoryName: String) -> Unit
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val showTimePicker = { onTimeSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        TimePickerDialog(context, { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val disabledFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val isFormValid = selectedCategory != null && startTime.isNotBlank() && endTime.isNotBlank() && selectedDate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar clase a ${student.firstName}") },
        text = {
            Column {
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

                OutlinedTextField(
                    value = selectedDate, onValueChange = {},
                    label = { Text("Fecha *") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker() },
                    enabled = false, colors = disabledFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startTime, onValueChange = {},
                    label = { Text("Hora inicio *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { startTime = it } },
                    enabled = false, colors = disabledFieldColors
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endTime, onValueChange = {},
                    label = { Text("Hora fin *") },
                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker { endTime = it } },
                    enabled = false, colors = disabledFieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(startTime, endTime, selectedDate, selectedCategory!!.id, selectedCategory!!.name)
                },
                enabled = isFormValid
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// -----------------------------------------------------------------------------------------
// TEACHER INVOICES (sin cambios significativos)
// -----------------------------------------------------------------------------------------

@Composable
fun TeacherInvoicesContent() {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""

    var tabIndex by remember { mutableIntStateOf(0) }
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var filteredInvoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var dateFilter by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        db.collection("facturas").whereEqualTo("teacherUid", uid).get().addOnSuccessListener { res ->
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
            invoices = list
        }
    }

    LaunchedEffect(tabIndex, dateFilter, invoices) {
        filteredInvoices = invoices.filter { inv ->
            (if (tabIndex == 1) inv.paid else if (tabIndex == 2) !inv.paid else true) &&
            (dateFilter.isEmpty() || inv.date.contains(dateFilter))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Todas") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Pagadas") })
            Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Pendientes") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateFilter,
            onValueChange = { dateFilter = it },
            label = { Text("Filtrar por fecha") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredInvoices) { invoice ->
                InvoiceItem(invoice)
            }
        }
    }
}

@Composable
fun InvoiceItem(invoice: Invoice) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(invoice.studentName, fontWeight = FontWeight.Bold)
                Text("${invoice.totalAmount} \u20ac")
            }
            Text(invoice.date)
            Text(
                if (invoice.paid) "Pagada" else "Pendiente",
                color = if (invoice.paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
