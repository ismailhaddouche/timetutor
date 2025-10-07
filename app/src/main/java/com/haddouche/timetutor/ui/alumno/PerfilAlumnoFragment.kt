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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.haddouche.timetutor.R
import com.haddouche.timetutor.model.User
import com.squareup.picasso.Picasso
import android.widget.TextView

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
        val recyclerFacturas = view.findViewById<RecyclerView?>(R.id.recyclerFacturasAlumno)

        recyclerFacturas?.layoutManager = LinearLayoutManager(context)
        cargarFacturasAlumno { facturas ->
            recyclerFacturas?.adapter = FacturaAlumnoAdapter(facturas)
        }

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

    private fun cargarFacturasAlumno(onLoaded: (List<FacturaAlumno>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        db.collection("facturas")
            .whereEqualTo("uidAlumno", uid)
            .get()
            .addOnSuccessListener { result ->
                val facturas = result.map { doc ->
                    FacturaAlumno(
                        fecha = doc.getString("fecha") ?: "",
                        cantidad = doc.getDouble("cantidad") ?: 0.0,
                        pagada = doc.getBoolean("pagada") ?: false
                    )
                }
                onLoaded(facturas)
            }
    }

    data class FacturaAlumno(
        val fecha: String,
        val cantidad: Double,
        val pagada: Boolean
    )

    class FacturaAlumnoAdapter(private val facturas: List<FacturaAlumno>) : RecyclerView.Adapter<FacturaAlumnoAdapter.FacturaAlumnoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacturaAlumnoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_factura, parent, false)
            return FacturaAlumnoViewHolder(view)
        }
        override fun onBindViewHolder(holder: FacturaAlumnoViewHolder, position: Int) {
            holder.bind(facturas[position])
            holder.itemView.setOnClickListener {
                val factura = facturas[position]
                mostrarDetalleClases(factura)
            }
        }
        override fun getItemCount() = facturas.size

        private fun mostrarDetalleClases(factura: FacturaAlumno) {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: return
            db.collection("clases")
                .whereEqualTo("uidAlumno", uid)
                .whereGreaterThanOrEqualTo("fecha", factura.fecha)
                .get()
                .addOnSuccessListener { result ->
                    val clases = result.filter { it.getBoolean("asistenciaMarcada") == true }
                        .joinToString("\n") {
                            val fecha = it.getString("fecha") ?: ""
                            val horaInicio = it.getString("horaInicio") ?: ""
                            val horaFin = it.getString("horaFin") ?: ""
                            "- $fecha de $horaInicio a $horaFin"
                        }
                    val mensaje = if (clases.isEmpty()) "No hay clases asistidas en esta factura." else "Clases incluidas:\n$clases"
                    androidx.appcompat.app.AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Detalle de factura")
                        .setMessage(mensaje)
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
        }

        class FacturaAlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(factura: FacturaAlumno) {
                val fechaView = itemView.findViewById<TextView>(R.id.textFechaFactura)
                val cantidadView = itemView.findViewById<TextView>(R.id.textCantidadFactura)
                val estadoView = itemView.findViewById<TextView>(R.id.textEstadoFactura)
                fechaView.text = factura.fecha
                cantidadView.text = "€${factura.cantidad}"
                estadoView.text = if (factura.pagada) "Pagada" else "Pendiente"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            view?.findViewById<ImageView>(R.id.imageFoto)?.setImageURI(selectedImageUri)
        }
    }
}
