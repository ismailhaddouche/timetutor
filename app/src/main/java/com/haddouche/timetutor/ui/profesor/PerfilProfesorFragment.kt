package com.haddouche.timetutor.ui.profesor

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.haddouche.timetutor.R
import android.widget.EditText
import android.widget.Switch
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.model.User
import com.squareup.picasso.Picasso
import android.content.Intent
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class PerfilProfesorFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil_profesor, container, false)
        // Aviso de conectividad
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!online) {
            android.widget.Toast.makeText(context, "Estás sin conexión. Los cambios no se sincronizarán hasta que recuperes la red.", android.widget.Toast.LENGTH_LONG).show()
        }

        val nombreField = view.findViewById<EditText>(R.id.editNombre)
        val apellidosField = view.findViewById<EditText>(R.id.editApellidos)
        val telefonoField = view.findViewById<EditText>(R.id.editTelefono)
        val switchTelefono = view.findViewById<Switch>(R.id.switchMostrarTelefono)
        val switchEmail = view.findViewById<Switch>(R.id.switchMostrarEmail)
        val calendarioField = view.findViewById<EditText>(R.id.editCalendario)
        val emailField = view.findViewById<EditText>(R.id.editEmail)
        val fotoView = view.findViewById<ImageView>(R.id.imageFoto)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarPerfil)

        val uid = auth.currentUser?.uid ?: ""
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val user = doc.toObject(User::class.java)
            if (user != null) {
                nombreField.setText(user.nombre)
                apellidosField.setText(user.apellidos)
                telefonoField.setText(user.telefono)
                switchTelefono.isChecked = user.mostrarTelefono
                switchEmail.isChecked = user.mostrarEmail
                calendarioField.setText(user.nombreCalendario)
                emailField.setText(user.email)
                if (user.fotoUrl.isNotEmpty()) {
                    Picasso.get().load(user.fotoUrl).into(fotoView)
                }
            }
        }

        fotoView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnGuardar.setOnClickListener {
            val email = emailField.text.toString()
            val telefono = telefonoField.text.toString()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Introduce un email válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (telefono.isNotBlank() && !android.util.Patterns.PHONE.matcher(telefono).matches()) {
                Toast.makeText(context, "Introduce un teléfono válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener { docs ->
                val esPropio = docs.any { it.id == uid }
                if (docs.size() > 0 && !esPropio) {
                    Toast.makeText(context, "Este email ya está en uso", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val userUpdate = User(
                    uid = uid,
                    email = email,
                    role = "profesor",
                    nombre = nombreField.text.toString(),
                    apellidos = apellidosField.text.toString(),
                    telefono = telefonoField.text.toString(),
                    mostrarTelefono = switchTelefono.isChecked,
                    mostrarEmail = switchEmail.isChecked,
                    nombreCalendario = calendarioField.text.toString(),
                    fotoUrl = ""
                )
                if (selectedImageUri != null) {
                    val storageRef = FirebaseStorage.getInstance().reference.child("fotos_perfil/$uid.jpg")
                    storageRef.putFile(selectedImageUri!!)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) throw task.exception!!
                            storageRef.downloadUrl
                        }
                        .addOnSuccessListener { uri ->
                            val userFinal = userUpdate.copy(fotoUrl = uri.toString())
                            db.collection("users").document(uid).set(userFinal)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                    // Notificación al profesor
                                    com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                                        uid,
                                        "Perfil actualizado",
                                        "Tu perfil de profesor ha sido actualizado correctamente."
                                    )
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al subir la foto", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    db.collection("users").document(uid).set(userUpdate)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                            // Notificación al profesor
                            com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                                uid,
                                "Perfil actualizado",
                                "Tu perfil de profesor ha sido actualizado correctamente."
                            )
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            view?.findViewById<ImageView>(R.id.imageFoto)?.setImageURI(selectedImageUri)
        }
    }
}
