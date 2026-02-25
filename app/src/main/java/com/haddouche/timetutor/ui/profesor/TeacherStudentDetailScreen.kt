package com.haddouche.timetutor.ui.profesor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haddouche.timetutor.model.Invoice
import com.haddouche.timetutor.model.Lesson
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.viewmodel.TeacherHomeUiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentDetailScreen(
    uiState: TeacherHomeUiState,
    onBack: () -> Unit,
    onToggleLessonSelection: (String) -> Unit,
    onGenerateInvoice: () -> Unit
) {
    val student = uiState.selectedStudent ?: return
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sin Facturar", "Programadas", "Facturadas", "Facturas", "Perfil")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${student.firstName} ${student.lastName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTabIndex) {
                    0 -> UnbilledLessonsTab(
                        lessons = uiState.studentLessons.filter { !it.isBilled && it.status == "impartida" },
                        selectedIds = uiState.selectedLessonsForInvoice,
                        onToggleSelection = onToggleLessonSelection,
                        onGenerateInvoice = onGenerateInvoice
                    )
                    1 -> ScheduledLessonsTab(
                        lessons = uiState.studentLessons.filter { it.status == "no_impartida" }
                    )
                    2 -> BilledLessonsTab(
                        lessons = uiState.studentLessons.filter { it.isBilled }
                    )
                    3 -> InvoicesTab(
                        invoices = uiState.studentInvoices
                    )
                    4 -> StudentProfileTab(student = student)
                }
            }
        }
    }
}

@Composable
fun UnbilledLessonsTab(
    lessons: List<Lesson>,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onGenerateInvoice: () -> Unit
) {
    Column {
        if (lessons.isEmpty()) {
            Text("No hay clases pendientes de facturar.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(lessons) { lesson ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(lesson.id),
                            onCheckedChange = { onToggleSelection(lesson.id) }
                        )
                        Column {
                            Text(text = "${lesson.date} - ${lesson.timeSlot}", fontWeight = FontWeight.Bold)
                            Text(text = lesson.categoryName)
                        }
                    }
                    HorizontalDivider()
                }
            }
            
            Button(
                onClick = onGenerateInvoice,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Generar Factura (${selectedIds.size})")
            }
        }
    }
}

@Composable
fun ScheduledLessonsTab(lessons: List<Lesson>) {
    LazyColumn {
        items(lessons) { lesson ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${lesson.date} - ${lesson.timeSlot}", fontWeight = FontWeight.Bold)
                    Text(text = lesson.categoryName)
                }
            }
        }
    }
}

@Composable
fun BilledLessonsTab(lessons: List<Lesson>) {
    LazyColumn {
        items(lessons) { lesson ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${lesson.date} - ${lesson.timeSlot}", fontWeight = FontWeight.Bold)
                    Text(text = lesson.categoryName)
                    Text(text = "Factura ID: ${lesson.invoiceId ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun InvoicesTab(invoices: List<Invoice>) {
    LazyColumn {
        items(invoices) { invoice ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Fecha: ${invoice.date}", fontWeight = FontWeight.Bold)
                    Text(text = "Total: ${String.format(Locale.getDefault(), "%.2f", invoice.totalAmount)} €")
                    Text(
                        text = if (invoice.paid) "Pagada" else "Pendiente",
                        color = if (invoice.paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun StudentProfileTab(student: User) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Nombre: ${student.firstName} ${student.lastName}", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Email: ${student.email}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Teléfono: ${student.phone}")
    }
}
