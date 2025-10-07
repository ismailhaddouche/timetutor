package com.haddouche.timetutor.ui.profesor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.haddouche.timetutor.R
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.model.User
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class AlumnosProfesorFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var alumnosActivos = listOf<User>()
    private var alumnosPendientes = listOf<User>()
    private var alumnosEliminados = listOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alumnos_profesor, container, false)
        // Aviso de conectividad
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!online) {
            android.widget.Toast.makeText(context, "Estás sin conexión. Los cambios no se sincronizarán hasta que recuperes la red.", android.widget.Toast.LENGTH_LONG).show()
        }

        tabLayout = view.findViewById(R.id.tabAlumnos)
        recyclerView = view.findViewById(R.id.recyclerAlumnos)
        recyclerView.layoutManager = LinearLayoutManager(context)

        tabLayout.addTab(tabLayout.newTab().setText("Activos"))
        tabLayout.addTab(tabLayout.newTab().setText("Pendientes"))
        tabLayout.addTab(tabLayout.newTab().setText("Eliminados"))

        cargarAlumnos {
            mostrarAlumnos(0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mostrarAlumnos(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        return view
    }

    private fun cargarAlumnos(onLoaded: () -> Unit) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        db.collection("users")
            .whereEqualTo("role", "alumno")
            .get()
            .addOnSuccessListener { result ->
                val activos = mutableListOf<User>()
                val pendientes = mutableListOf<User>()
                val eliminados = mutableListOf<User>()
                for (doc in result) {
                    val user = doc.toObject(User::class.java)
                    when (doc.getString("estadoAlumno_$uidProfesor")) {
                        "activo" -> activos.add(user)
                        "pendiente" -> pendientes.add(user)
                        "eliminado" -> eliminados.add(user)
                    }
                }
                alumnosActivos = activos
                alumnosPendientes = pendientes
                alumnosEliminados = eliminados
                onLoaded()
            }
    }

    private fun mostrarAlumnos(tabIndex: Int) {
        val alumnos = when (tabIndex) {
            0 -> alumnosActivos
            1 -> alumnosPendientes
            2 -> alumnosEliminados
            else -> alumnosActivos
        }
        recyclerView.adapter = AlumnoAdapter(alumnos, tabIndex, ::onAlumnoAction)
    }

    private fun onAlumnoAction(alumno: User, tabIndex: Int, action: String) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        val docRef = db.collection("users").document(alumno.uid)
        when (action) {
            "aceptar" -> {
                docRef.update("estadoAlumno_$uidProfesor", "activo")
                    .addOnSuccessListener {
                        // Notificar al alumno que ha sido aceptado
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            alumno.uid,
                            "Solicitud aceptada",
                            "Tu solicitud ha sido aceptada por el profesor. Ya puedes acceder a tus clases."
                        )
                        cargarAlumnos { mostrarAlumnos(tabIndex) }
                    }
            }
            "eliminar" -> {
                docRef.update("estadoAlumno_$uidProfesor", "eliminado")
                    .addOnSuccessListener {
                        // Notificar al alumno que ha sido eliminado
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            alumno.uid,
                            "Eliminado por el profesor",
                            "Has sido eliminado por el profesor. Si crees que es un error, contacta con tu profesor."
                        )
                        cargarAlumnos { mostrarAlumnos(tabIndex) }
                    }
            }
            "asignar" -> {
                mostrarDialogoEditarClase(alumno)
            }
            "info" -> {
                mostrarInfoAlumno(alumno)
            }
        }
    }

    private fun mostrarDialogoEditarClase(alumno: User) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_agregar_clase, null, false)
        val editHoraInicio = dialogView.findViewById<android.widget.EditText>(R.id.editHoraInicio)
        val editHoraFin = dialogView.findViewById<android.widget.EditText>(R.id.editHoraFin)
        // Spinner oculto, ya que el alumno está preseleccionado
        dialogView.findViewById<android.widget.Spinner>(R.id.spinnerAlumno)?.visibility = View.GONE

        // Selección de hora
        val horaListener = { edit: android.widget.EditText ->
            val now = Calendar.getInstance()
            android.app.TimePickerDialog(requireContext(), { _, hour, minute ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                edit.setText(sdf.format(cal.time))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        editHoraInicio.setOnClickListener { horaListener(editHoraInicio) }
        editHoraFin.setOnClickListener { horaListener(editHoraFin) }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar/Asignar clase a ${alumno.nombre}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val horaInicio = editHoraInicio.text.toString()
                val horaFin = editHoraFin.text.toString()
                if (horaInicio.isBlank() || horaFin.isBlank()) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
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
                // Preguntar si modificar solo una clase o todas las de ese día/hora
                preguntarEdicionClase(alumno, horaInicio, horaFin)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun preguntarEdicionClase(alumno: User, horaInicio: String, horaFin: String) {
        val opciones = arrayOf(
            "Modificar solo la próxima clase",
            "Modificar todas las clases de este día y hora (repetidas semanalmente)"
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("¿Qué deseas modificar?")
            .setItems(opciones) { _, which ->
                if (which == 0) {
                    modificarSoloProximaClase(alumno, horaInicio, horaFin)
                } else {
                    modificarTodasClasesRecurrentes(alumno, horaInicio, horaFin)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun modificarSoloProximaClase(alumno: User, horaInicio: String, horaFin: String) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        val db = FirebaseFirestore.getInstance()
        // Buscar la próxima clase futura del alumno con el profesor
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        db.collection("clases")
            .whereEqualTo("uidProfesor", uidProfesor)
            .whereEqualTo("uidAlumno", alumno.uid)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .orderBy("fecha")
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val claseDoc = result.documents.firstOrNull()
                if (claseDoc != null) {
                    // Modificar la clase existente
                    claseDoc.reference.update(
                        mapOf(
                            "horaInicio" to horaInicio,
                            "horaFin" to horaFin
                        )
                    ).addOnSuccessListener {
                        // Notificaciones
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            alumno.uid,
                            "Clase modificada",
                            "Se ha modificado una clase en tu horario."
                        )
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            uidProfesor,
                            "Clase modificada",
                            "Has modificado una clase de ${alumno.nombre}."
                        )
                        Toast.makeText(context, "Clase modificada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Si no hay clase futura, crearla
                    val nuevaClase = hashMapOf(
                        "uidProfesor" to uidProfesor,
                        "uidAlumno" to alumno.uid,
                        "nombreAlumno" to alumno.nombre,
                        "horaInicio" to horaInicio,
                        "horaFin" to horaFin,
                        "fecha" to hoy,
                        "asistenciaMarcada" to false
                    )
                    db.collection("clases").add(nuevaClase).addOnSuccessListener {
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            alumno.uid,
                            "Clase añadida",
                            "Se ha añadido una clase en tu horario."
                        )
                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                            uidProfesor,
                            "Clase añadida",
                            "Has añadido una clase para ${alumno.nombre}."
                        )
                        Toast.makeText(context, "Clase creada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun modificarTodasClasesRecurrentes(alumno: User, horaInicio: String, horaFin: String) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        val db = FirebaseFirestore.getInstance()
        // Buscar todas las clases futuras del alumno con el profesor que coincidan en día de la semana y hora
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val diaSemanaHoy = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        db.collection("clases")
            .whereEqualTo("uidProfesor", uidProfesor)
            .whereEqualTo("uidAlumno", alumno.uid)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .get()
            .addOnSuccessListener { result ->
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val clasesFiltradas = result.documents.filter { doc ->
                    val fecha = doc.getString("fecha") ?: return@filter false
                    val horaIni = doc.getString("horaInicio") ?: return@filter false
                    val cal = java.util.Calendar.getInstance()
                    try {
                        cal.time = sdf.parse(fecha) ?: return@filter false
                    } catch (_: Exception) { return@filter false }
                    val diaSemanaClase = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    // Coincide día de la semana y hora de inicio
                    diaSemanaClase == diaSemanaHoy && horaIni == horaInicio
                }
                for (doc in clasesFiltradas) {
                    doc.reference.update(
                        mapOf(
                            "horaInicio" to horaInicio,
                            "horaFin" to horaFin
                        )
                    )
                }
                if (clasesFiltradas.isNotEmpty()) {
                    com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                        alumno.uid,
                        "Clases modificadas",
                        "Se han modificado varias clases en tu horario."
                    )
                    com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                        uidProfesor,
                        "Clases modificadas",
                        "Has modificado varias clases de ${alumno.nombre}."
                    )
                    Toast.makeText(context, "Clases modificadas", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se encontraron clases recurrentes para modificar", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mostrarInfoAlumno(alumno: User) {
        db.collection("facturas")
            .whereEqualTo("uidProfesor", auth.currentUser?.uid ?: "")
            .whereEqualTo("nombreAlumno", alumno.nombre)
            .get()
            .addOnSuccessListener { result ->
                val facturas = result.map {
                    val pagada = it.getBoolean("pagada") ?: false
                    val fecha = it.getString("fecha") ?: ""
                    val cantidad = it.getDouble("cantidad") ?: 0.0
                    "${fecha}: €${cantidad} - " + if (pagada) "Pagada" else "Pendiente"
                }
                val info = "Nombre: ${alumno.nombre}\nEmail: ${alumno.email}\nTeléfono: ${alumno.telefono}\n\nFacturas:\n" +
                    if (facturas.isEmpty()) "Sin facturas" else facturas.joinToString("\n")
                AlertDialog.Builder(requireContext())
                    .setTitle("Información de alumno")
                    .setMessage(info)
                    .setPositiveButton("Generar factura") { _, _ ->
                        generarFacturaManual(alumno)
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
    }

    private fun generarFacturaManual(alumno: User) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        // Buscar la última factura
        db.collection("facturas")
            .whereEqualTo("uidProfesor", uidProfesor)
            .whereEqualTo("uidAlumno", alumno.uid)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { factSnap ->
                val ultimaFecha = factSnap.documents.firstOrNull()?.getString("fecha") ?: "1970-01-01"
                // Buscar clases desde la última factura
                db.collection("clases")
                    .whereEqualTo("uidProfesor", uidProfesor)
                    .whereEqualTo("uidAlumno", alumno.uid)
                    .whereGreaterThan("fecha", ultimaFecha)
                    .get()
                    .addOnSuccessListener { clasesSnap ->
                        val clases = clasesSnap.map {
                            val asistenciaMarcada = it.getBoolean("asistenciaMarcada") ?: false
                            val duracionMin = it.getLong("duracionMin")?.toInt() ?: 60 // duración en minutos
                            ClaseDeAlumno(
                                nombreAlumno = alumno.nombre,
                                fecha = it.getString("fecha") ?: "",
                                asistenciaMarcada = asistenciaMarcada,
                                duracionMin = duracionMin
                            )
                        }
                        if (clases.any { !it.asistenciaMarcada }) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Clases sin marcar asistencia")
                                .setMessage("Debes marcar la asistencia de todas las clases antes de generar la factura.")
                                .setPositiveButton("OK", null)
                                .show()
                        } else {
                            // Obtener precio por hora de la categoría
                            db.collection("categorias")
                                .whereEqualTo("uidProfesor", uidProfesor)
                                .whereEqualTo("nombre", alumno.categoria)
                                .get()
                                .addOnSuccessListener { catSnap ->
                                    val precioHora = catSnap.documents.firstOrNull()?.getDouble("precioMediaHora")?.times(2) ?: 0.0
                                    val totalMin = clases.sumOf { it.duracionMin }
                                    val cantidad = (totalMin / 60.0) * precioHora
                                    val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    db.collection("facturas").add(
                                        mapOf(
                                            "uidProfesor" to uidProfesor,
                                            "uidAlumno" to alumno.uid,
                                            "nombreAlumno" to alumno.nombre,
                                            "apellidosAlumno" to alumno.apellidos,
                                            "whatsappAlumno" to alumno.telefono,
                                            "emailAlumno" to alumno.email,
                                            "fecha" to fechaHoy,
                                            "cantidad" to cantidad,
                                            "pagada" to false
                                        )
                                    ).addOnSuccessListener {
                                        Toast.makeText(context, "Factura generada", Toast.LENGTH_SHORT).show()
                                        // Notificar al alumno
                                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                                            alumno.uid,
                                            "Nueva factura generada",
                                            "El profesor ha generado una nueva factura correspondiente a tus clases."
                                        )
                                        // Notificar al profesor
                                        com.haddouche.timetutor.util.NotificacionesUtil.enviarNotificacion(
                                            uidProfesor,
                                            "Factura generada",
                                            "Has generado una nueva factura para ${alumno.nombre}."
                                        )
                                    }
                                }
                        }
                    }
            }
    }

}

class AlumnoAdapter(
    private val alumnos: List<User>,
    private val tabIndex: Int,
    private val onAction: (User, Int, String) -> Unit
) : RecyclerView.Adapter<AlumnoAdapter.AlumnoViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlumnoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alumno, parent, false)
        return AlumnoViewHolder(view)
    }
    override fun onBindViewHolder(holder: AlumnoViewHolder, position: Int) {
        holder.bind(alumnos[position], tabIndex, onAction)
    }
    override fun getItemCount() = alumnos.size

    class AlumnoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(alumno: User, tabIndex: Int, onAction: (User, Int, String) -> Unit) {
            val nombreView = itemView.findViewById<TextView>(R.id.textNombreAlumno)
            val asignaturaView = itemView.findViewById<TextView>(R.id.textAsignaturaAlumno)
            val btnAccion = itemView.findViewById<Button>(R.id.btnAccionAlumno)
            nombreView.text = alumno.nombre
            asignaturaView.text = alumno.nombreCalendario
            itemView.setOnClickListener { onAction(alumno, tabIndex, "info") }
            when (tabIndex) {
                0 -> {
                    btnAccion.text = "Asignar clase"
                    btnAccion.setOnClickListener { onAction(alumno, tabIndex, "asignar") }
                }
                1 -> {
                    btnAccion.text = "Aceptar"
                    btnAccion.setOnClickListener { onAction(alumno, tabIndex, "aceptar") }
                }
                2 -> {
                    btnAccion.text = "Restaurar"
                    btnAccion.setOnClickListener { onAction(alumno, tabIndex, "aceptar") }
                }
            }
            val btnEliminar = itemView.findViewById<Button>(R.id.btnEliminarAlumno)
            btnEliminar.setOnClickListener { onAction(alumno, tabIndex, "eliminar") }
        }
    }
}

data class ClaseDeAlumno(
    val nombreAlumno: String = "",
    val fecha: String = "",
    val asistenciaMarcada: Boolean = false,
    val duracionMin: Int = 60
)
