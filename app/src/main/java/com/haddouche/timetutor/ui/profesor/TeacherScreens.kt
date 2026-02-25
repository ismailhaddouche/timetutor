package com.haddouche.timetutor.ui.profesor

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.haddouche.timetutor.model.User
import com.haddouche.timetutor.ui.common.UserProfileImage
import com.haddouche.timetutor.viewmodel.Category

@Composable
fun TeacherProfileScreen(
    profile: User?,
    onSaveProfile: (String, String, String, Boolean, Boolean, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var firstName by remember(profile) { mutableStateOf(profile?.firstName ?: "") }
    var lastName by remember(profile) { mutableStateOf(profile?.lastName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var showPhone by remember(profile) { mutableStateOf(profile?.showPhone ?: false) }
    var showEmail by remember(profile) { mutableStateOf(profile?.showEmail ?: false) }
    var calendarName by remember(profile) { mutableStateOf(profile?.calendarName ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
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
                    size = 120.dp,
                    fontSize = 40.sp
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

        OutlinedTextField(value = calendarName, onValueChange = { calendarName = it }, label = { Text("Nombre del Calendario") }, modifier = Modifier.fillMaxWidth())

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
                            onSaveProfile(firstName, lastName, phone, showPhone, showEmail, calendarName, email, photoUrl)
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al subir foto", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    onSaveProfile(firstName, lastName, phone, showPhone, showEmail, calendarName, email, photoUrl)
                    Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Cambios")
        }
    }
}

@Composable
fun TeacherSettingsScreen(
    categories: List<Category> = emptyList(),
    onAddCategory: (String, Double) -> Unit = { _, _ -> },
    onLogOut: () -> Unit,
    onThemeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryPrice by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        Text("Configuracion", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Tema de la aplicacion", style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = { showThemeDialog = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Cambiar tema")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Categorias de Precios", style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Nombre") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = newCategoryPrice,
                onValueChange = { newCategoryPrice = it },
                label = { Text("Precio/h") },
                modifier = Modifier.width(100.dp)
            )
        }
        Button(
            onClick = {
                val price = newCategoryPrice.toDoubleOrNull()
                if (newCategoryName.isNotBlank() && price != null && price > 0) {
                    onAddCategory(newCategoryName, price)
                    newCategoryName = ""
                    newCategoryPrice = ""
                } else {
                    Toast.makeText(context, "Datos invalidos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Anadir Categoria")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category.name)
                        Text("${category.hourlyRate} \u20ac/h")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
