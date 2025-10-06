package com.haddouche.timetutor.ui.alumno

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.haddouche.timetutor.R
import com.haddouche.timetutor.model.User
import com.squareup.picasso.Picasso

class PerfilAlumnoFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE = 1002

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil_alumno, container, false)
        val nombreField = view.findViewById<EditText>(R.id.editNombre)
        val apellidosField = view.findViewById<EditText>(R.id.editApellidos)
        val telefonoField = view.findViewById<EditText>(R.id.editTelefono)
        val switchTelefono = view.findViewById<Switch>(R.id.switchMostrarTelefono)
        val switchEmail = view.findViewById<Switch>(R.id.switchMostrarEmail)
        val emailField = view.findViewById<EditText>(R.id.editEmail)
        val fotoView = view.findViewById<ImageView>(R.id.imageFoto)
        val whatsappTutorField = view.findViewById<EditText>(R.id.editWhatsappTutor)
        val correoTutorField = view.findViewById<EditText>(R.id.editCorreoTutor)
        val categoriaField = view.findViewById<EditText>(R.id.editCategoria)
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
                emailField.setText(user.email)
                whatsappTutorField.setText(user.whatsappTutor)
                correoTutorField.setText(user.correoTutor)
                categoriaField.setText(user.categoria)
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
            val userUpdate = User(
                uid = uid,
                email = emailField.text.toString(),
                role = "alumno",
                nombre = nombreField.text.toString(),
                apellidos = apellidosField.text.toString(),
                telefono = telefonoField.text.toString(),
                mostrarTelefono = switchTelefono.isChecked,
                mostrarEmail = switchEmail.isChecked,
                nombreCalendario = "",
                fotoUrl = "",
                whatsappTutor = whatsappTutorField.text.toString(),
                correoTutor = correoTutorField.text.toString(),
                categoria = categoriaField.text.toString().ifBlank { "Sin categoría" }
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
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
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
