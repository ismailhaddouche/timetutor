package com.haddouche.timetutor.ui.profesor

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.haddouche.timetutor.R
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import java.text.SimpleDateFormat
import java.util.*
import com.haddouche.timetutor.util.NotificacionesUtil

class InicioProfesorFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio_profesor, container, false)
        // Aviso de conectividad
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!online) {
            android.widget.Toast.makeText(context, "Estás sin conexión. Los cambios no se sincronizarán hasta que recuperes la red.", android.widget.Toast.LENGTH_LONG).show()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerClasesHoy)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val clasesHoy = getClasesHoyMock()
        recyclerView.adapter = ClaseAdapter(clasesHoy) { clase ->
            Toast.makeText(context, "Alumnos: " + clase.alumnos.joinToString(", ") { it.nombre }, Toast.LENGTH_LONG).show()
        }

        val btnAgregarClase = view.findViewById<Button>(R.id.btnAgregarClase)
        val btnVistaSemanal = view.findViewById<Button>(R.id.btnVistaSemanal)
        btnAgregarClase.setOnClickListener {
            mostrarDialogoAgregarClase()
        }
        btnVistaSemanal.setOnClickListener {
            Toast.makeText(context, "Cambiando a vista semanal", Toast.LENGTH_SHORT).show()
        }
        // Botón para acceder al centro de notificaciones
        val btnNotificaciones = view.findViewById<View?>(R.id.btnNotificacionesProfesor)
        btnNotificaciones?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(id, com.haddouche.timetutor.ui.common.NotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }
        return view
    }

    private fun mostrarDialogoAgregarClase() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_agregar_clase, null, false)
        val spinnerAlumno = dialogView.findViewById<Spinner>(R.id.spinnerAlumno)
        val editHoraInicio = dialogView.findViewById<EditText>(R.id.editHoraInicio)
        val editHoraFin = dialogView.findViewById<EditText>(R.id.editHoraFin)

        // Obtener alumnos reales del profesor
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val uidProfesor = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val alumnos = mutableListOf<String>()
        val alumnosUid = mutableListOf<String>()
        db.collection("users")
            .whereEqualTo("role", "alumno")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    if (doc.getString("estadoAlumno_$uidProfesor") == "activo") {
                        alumnos.add(doc.getString("nombre") ?: "")
                        alumnosUid.add(doc.id)
                    }
                }
                spinnerAlumno.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, alumnos)
            }

        val horaListener = { edit: EditText ->
            val now = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                edit.setText(sdf.format(cal.time))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        editHoraInicio.setOnClickListener { horaListener(editHoraInicio) }
        editHoraFin.setOnClickListener { horaListener(editHoraFin) }

        AlertDialog.Builder(requireContext())
            .setTitle("Añadir clase")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val alumno = spinnerAlumno.selectedItem?.toString() ?: ""
                val idx = spinnerAlumno.selectedItemPosition
                val uidAlumno = if (idx >= 0 && idx < alumnosUid.size) alumnosUid[idx] else null
                val horaInicio = editHoraInicio.text.toString()
                val horaFin = editHoraFin.text.toString()
                if (alumno.isBlank() || horaInicio.isBlank() || horaFin.isBlank() || uidAlumno == null) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateInicio = sdf.parse(horaInicio)
                val dateFin = sdf.parse(horaFin)
                if (dateInicio == null || dateFin == null) {
                    Toast.makeText(context, "Hora no válida", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val diff = (dateFin.time - dateInicio.time) / (1000 * 60)
                if (diff < 30) {
                    Toast.makeText(context, "La clase debe durar al menos 30 minutos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Guardar la clase en Firestore (ejemplo básico)
                val clase = hashMapOf(
                    "uidProfesor" to uidProfesor,
                    "uidAlumno" to uidAlumno,
                    "nombreAlumno" to alumno,
                    "horaInicio" to horaInicio,
                    "horaFin" to horaFin,
                    "fecha" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()),
                    "asistenciaMarcada" to false
                )
                db.collection("clases").add(clase).addOnSuccessListener {
                    // Notificación al alumno
                    NotificacionesUtil.enviarNotificacion(
                        uidDestino = uidAlumno,
                        titulo = "Clase añadida o modificada",
                        mensaje = "Se ha añadido o modificado una clase en tu horario."
                    )
                    // Notificación al profesor
                    NotificacionesUtil.enviarNotificacion(
                        uidDestino = uidProfesor,
                        titulo = "Clase añadida o modificada",
                        mensaje = "Has añadido o modificado una clase."
                    )
                    Toast.makeText(context, "Clase añadida para $alumno de $horaInicio a $horaFin", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()

    // Función para eliminar una clase (ejemplo genérico)
    fun eliminarClase(idClase: String, uidAlumno: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val uidProfesor = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.collection("clases").document(idClase).delete().addOnSuccessListener {
            // Notificación al alumno
            NotificacionesUtil.enviarNotificacion(
                uidDestino = uidAlumno,
                titulo = "Clase eliminada",
                mensaje = "Una clase ha sido eliminada de tu horario."
            )
            // Notificación al profesor
            NotificacionesUtil.enviarNotificacion(
                uidDestino = uidProfesor,
                titulo = "Clase eliminada",
                mensaje = "Has eliminado una clase."
            )
        }
    }
    }

    // Mock temporal para demo visual
    private fun getClasesHoyMock(): List<ClaseHoy> {
        return listOf(
            ClaseHoy("16:00 - 17:30", listOf(Alumno("Juan"), Alumno("Pep"))),
            ClaseHoy("17:30 - 19:00", listOf(Alumno("Maria"))),
            ClaseHoy("19:00 - 20:00", listOf(Alumno("Luis"), Alumno("Ana")))
        )
    }
}

// Modelos para demo
data class ClaseHoy(val franja: String, val alumnos: List<Alumno>, var asistencia: Boolean = false)
data class Alumno(val nombre: String)

class ClaseAdapter(
    private val clases: List<ClaseHoy>,
    private val onClick: (ClaseHoy) -> Unit
) : RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clase_hoy, parent, false)
        return ClaseViewHolder(view)
    }
    override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
        holder.bind(clases[position], onClick)
    }
    override fun getItemCount() = clases.size

    class ClaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(clase: ClaseHoy, onClick: (ClaseHoy) -> Unit) {
            val franjaView = itemView.findViewById<TextView>(R.id.textFranja)
            val alumnosView = itemView.findViewById<TextView>(R.id.textAlumnos)
            val checkAsistencia = itemView.findViewById<CheckBox?>(R.id.checkAsistencia)
            franjaView.text = clase.franja
            alumnosView.text = "Alumnos: " + clase.alumnos.joinToString(", ") { it.nombre }
            checkAsistencia?.isChecked = clase.asistencia
            checkAsistencia?.setOnCheckedChangeListener { _, isChecked ->
                clase.asistencia = isChecked
            }
            itemView.setOnClickListener { onClick(clase) }
        }
    }
}

// Lógica para facturación (ejemplo de validación)
private fun puedeGenerarFacturaParaAlumno(alumno: Alumno, clases: List<ClaseHoy>): Boolean {
    return clases.filter { it.alumnos.contains(alumno) }.all { it.asistencia }
}
