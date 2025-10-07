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
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FacturasProfesorFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerAlumnos: Spinner
    private lateinit var editFecha: EditText
    private var facturasTodas = listOf<Factura>()
    private var facturasPagadas = listOf<Factura>()
    private var facturasPendientes = listOf<Factura>()
    private var alumnos = listOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_facturas_profesor, container, false)
        tabLayout = view.findViewById(R.id.tabFacturas)
        recyclerView = view.findViewById(R.id.recyclerFacturas)
        spinnerAlumnos = view.findViewById(R.id.spinnerAlumnos)
        editFecha = view.findViewById(R.id.editFecha)
        recyclerView.layoutManager = LinearLayoutManager(context)

        tabLayout.addTab(tabLayout.newTab().setText("Todas"))
        tabLayout.addTab(tabLayout.newTab().setText("Pagadas"))
        tabLayout.addTab(tabLayout.newTab().setText("Pendientes"))

        cargarAlumnos {
            cargarFacturas {
                mostrarFacturas(0)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mostrarFacturas(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        spinnerAlumnos.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                mostrarFacturas(tabLayout.selectedTabPosition)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        editFecha.setOnEditorActionListener { _, _, _ ->
            mostrarFacturas(tabLayout.selectedTabPosition)
            true
        }
        return view
    }

    private fun cargarAlumnos(onLoaded: () -> Unit) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        db.collection("users")
            .whereEqualTo("role", "alumno")
            .get()
            .addOnSuccessListener { result ->
                alumnos = result.map { it.getString("nombre") ?: "" }
                spinnerAlumnos.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Todos") + alumnos)
                onLoaded()
            }
    }

    private fun cargarFacturas(onLoaded: () -> Unit) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        db.collection("facturas")
            .whereEqualTo("uidProfesor", uidProfesor)
            .get()
            .addOnSuccessListener { result ->
                val todas = mutableListOf<Factura>()
                val pagadas = mutableListOf<Factura>()
                val pendientes = mutableListOf<Factura>()
                for (doc in result) {
                    val factura = doc.toObject(Factura::class.java)
                    todas.add(factura)
                    if (factura.pagada) pagadas.add(factura) else pendientes.add(factura)
                }
                facturasTodas = todas
                facturasPagadas = pagadas
                facturasPendientes = pendientes
                onLoaded()
            }
    }

    private fun mostrarFacturas(tabIndex: Int) {
        val alumnoFiltro = spinnerAlumnos.selectedItem?.toString() ?: "Todos"
        val fechaFiltro = editFecha.text.toString().trim()
        val facturas = when (tabIndex) {
            0 -> facturasTodas
            1 -> facturasPagadas
            2 -> facturasPendientes
            else -> facturasTodas
        }.filter {
            (alumnoFiltro == "Todos" || it.nombreAlumno == alumnoFiltro) &&
            (fechaFiltro.isEmpty() || it.fecha.contains(fechaFiltro))
        }
        recyclerView.adapter = FacturaAdapter(facturas)
    }

    // Lógica para comprobar si se puede facturar
    private fun puedeGenerarFacturaParaAlumno(alumno: String, clases: List<ClaseDeAlumno>): Boolean {
        return clases.filter { it.nombreAlumno == alumno }.all { it.asistenciaMarcada }
    }

    private fun intentarGenerarFactura(alumno: String, clases: List<ClaseDeAlumno>) {
        if (!puedeGenerarFacturaParaAlumno(alumno, clases)) {
            Toast.makeText(context, "No se puede generar factura: hay clases sin marcar asistencia para $alumno", Toast.LENGTH_LONG).show()
            // Aquí podrías lanzar una notificación o bloquear el botón de facturación
            return
        }
        // Lógica para generar factura
    }

    private fun recargarFacturas() {
        cargarFacturas {
            mostrarFacturas(tabLayout.selectedTabPosition)
        }
    }

    inner class FacturaAdapter(private val facturas: List<Factura>) : RecyclerView.Adapter<FacturaAdapter.FacturaViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacturaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_factura, parent, false)
            return FacturaViewHolder(view)
        }
        override fun onBindViewHolder(holder: FacturaViewHolder, position: Int) {
            holder.bind(facturas[position])
            holder.itemView.setOnClickListener {
                val factura = facturas[position]
                val detalle = "Alumno: ${factura.nombreAlumno} ${factura.apellidosAlumno ?: ""}\n" +
                    "WhatsApp: ${factura.whatsappAlumno ?: ""}\n" +
                    "Email: ${factura.emailAlumno ?: ""}\n" +
                    "Fecha: ${factura.fecha}\n" +
                    "Cantidad: €${factura.cantidad}\n" +
                    "Estado: ${if (factura.pagada) "Pagada" else "Pendiente"}"
                val builder = androidx.appcompat.app.AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Detalle de factura")
                    .setMessage(detalle)
                    .setPositiveButton("Cerrar", null)
                if (!factura.pagada) {
                    builder.setNegativeButton("Marcar como pagada") { _, _ ->
                        marcarFacturaComoPagada(factura)
                    }
                }
                builder.show()
            }
        }
        override fun getItemCount() = facturas.size

        class FacturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(factura: Factura) {
                val nombreView = itemView.findViewById<TextView>(R.id.textNombreFactura)
                val fechaView = itemView.findViewById<TextView>(R.id.textFechaFactura)
                val cantidadView = itemView.findViewById<TextView>(R.id.textCantidadFactura)
                val estadoView = itemView.findViewById<TextView>(R.id.textEstadoFactura)
                nombreView.text = factura.nombreAlumno
                fechaView.text = factura.fecha
                cantidadView.text = "€${factura.cantidad}"
                estadoView.text = if (factura.pagada) "Pagada" else "Pendiente"
            }
        }

        private fun marcarFacturaComoPagada(factura: Factura) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("facturas")
                .whereEqualTo("uidProfesor", factura.uidProfesor)
                .whereEqualTo("uidAlumno", factura.uidAlumno)
                .whereEqualTo("fecha", factura.fecha)
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap) {
                        doc.reference.update("pagada", true)
                    }
                    android.widget.Toast.makeText(
                        holder?.itemView?.context ?: return@addOnSuccessListener,
                        "Factura marcada como pagada",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    recargarFacturas()
                }
        }
    }
}

data class Factura(
    val uidProfesor: String = "",
    val uidAlumno: String = "",
    val nombreAlumno: String = "",
    val apellidosAlumno: String? = null,
    val whatsappAlumno: String? = null,
    val emailAlumno: String? = null,
    val fecha: String = "",
    val cantidad: Double = 0.0,
    val pagada: Boolean = false
)

data class ClaseDeAlumno(
    val nombreAlumno: String = "",
    val fecha: String = "",
    val asistenciaMarcada: Boolean = false
)
