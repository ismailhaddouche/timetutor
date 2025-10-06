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
                    .addOnSuccessListener { cargarAlumnos { mostrarAlumnos(tabIndex) } }
            }
            "eliminar" -> {
                docRef.update("estadoAlumno_$uidProfesor", "eliminado")
                    .addOnSuccessListener { cargarAlumnos { mostrarAlumnos(tabIndex) } }
            }
            "asignar" -> {
                Toast.makeText(context, "Asignar clase a ${alumno.nombre}", Toast.LENGTH_SHORT).show()
                // Aquí iría la lógica para asignar clase
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
