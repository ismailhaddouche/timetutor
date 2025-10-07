package com.haddouche.timetutor.ui.alumno

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
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FacturasAlumnoFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var editFecha: EditText
    private var facturasTodas = listOf<Factura>()
    private var facturasPagadas = listOf<Factura>()
    private var facturasPendientes = listOf<Factura>()
    private var exportando = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_facturas_alumno, container, false)
        tabLayout = view.findViewById(R.id.tabFacturasAlumno)
        recyclerView = view.findViewById(R.id.recyclerFacturasAlumno)
        editFecha = view.findViewById(R.id.editFechaAlumno)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Aviso de conectividad
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val online = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!online) {
            android.widget.Toast.makeText(context, "Estás sin conexión. Los cambios no se sincronizarán hasta que recuperes la red.", android.widget.Toast.LENGTH_LONG).show()
        }

        tabLayout.addTab(tabLayout.newTab().setText("Todas"))
        tabLayout.addTab(tabLayout.newTab().setText("Pagadas"))
        tabLayout.addTab(tabLayout.newTab().setText("Pendientes"))

        cargarFacturas {
            mostrarFacturas(0)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mostrarFacturas(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        editFecha.setOnEditorActionListener { _, _, _ ->
            mostrarFacturas(tabLayout.selectedTabPosition)
            true
        }

        val btnExportar = view.findViewById<android.widget.Button>(R.id.btnExportarFacturasAlumno)
        btnExportar.setOnClickListener {
            if (!exportando) mostrarDialogoExportar()
        }
        return view
    }

    private fun cargarFacturas(onLoaded: () -> Unit) {
        val uidAlumno = auth.currentUser?.uid ?: ""
        db.collection("facturas")
            .whereEqualTo("uidAlumno", uidAlumno)
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
        val fechaFiltro = editFecha.text.toString().trim()
        val facturas = when (tabIndex) {
            0 -> facturasTodas
            1 -> facturasPagadas
            2 -> facturasPendientes
            else -> facturasTodas
        }.filter {
            fechaFiltro.isEmpty() || it.fecha.contains(fechaFiltro)
        }
        recyclerView.adapter = FacturaAdapter(facturas)
    }

    private fun mostrarDialogoExportar() {
        val opciones = arrayOf("Exportar a PDF", "Exportar a CSV")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Exportar facturas")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> exportarFacturasPDF()
                    1 -> exportarFacturasCSV()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun obtenerFacturasFiltradas(): List<Factura> {
        val tabIndex = tabLayout.selectedTabPosition
        val fechaFiltro = editFecha.text.toString().trim()
        return when (tabIndex) {
            0 -> facturasTodas
            1 -> facturasPagadas
            2 -> facturasPendientes
            else -> facturasTodas
        }.filter {
            fechaFiltro.isEmpty() || it.fecha.contains(fechaFiltro)
        }
    }

    private fun exportarFacturasCSV() {
        exportando = true
        val facturas = obtenerFacturasFiltradas()
        if (facturas.isEmpty()) {
            Toast.makeText(context, "No hay facturas para exportar", Toast.LENGTH_SHORT).show()
            exportando = false
            return
        }
        val header = "Alumno,Apellidos,WhatsApp,Email,Fecha,Cantidad,Estado"
        val rows = facturas.joinToString("\n") {
            "${it.nombreAlumno},${it.apellidosAlumno ?: ""},${it.whatsappAlumno ?: ""},${it.emailAlumno ?: ""},${it.fecha},${it.cantidad},${if (it.pagada) "Pagada" else "Pendiente"}"
        }
        val csv = "$header\n$rows"
        try {
            val fileName = "facturas_alumno_${System.currentTimeMillis()}.csv"
            val file = java.io.File(requireContext().cacheDir, fileName)
            file.writeText(csv)
            compartirArchivo(file, "text/csv")
        } catch (e: Exception) {
            Toast.makeText(context, "Error exportando CSV", Toast.LENGTH_SHORT).show()
        }
        exportando = false
    }

    private fun exportarFacturasPDF() {
        exportando = true
        val facturas = obtenerFacturasFiltradas()
        if (facturas.isEmpty()) {
            Toast.makeText(context, "No hay facturas para exportar", Toast.LENGTH_SHORT).show()
            exportando = false
            return
        }
        try {
            val fileName = "facturas_alumno_${System.currentTimeMillis()}.pdf"
            val file = java.io.File(requireContext().cacheDir, fileName)
            val fos = java.io.FileOutputStream(file)
            val pdf = com.itextpdf.text.Document()
            val writer = com.itextpdf.text.pdf.PdfWriter.getInstance(pdf, fos)
            pdf.open()
            pdf.add(com.itextpdf.text.Paragraph("Facturas exportadas"))
            pdf.add(com.itextpdf.text.Paragraph(" "))
            val table = com.itextpdf.text.pdf.PdfPTable(7)
            val headers = listOf("Alumno", "Apellidos", "WhatsApp", "Email", "Fecha", "Cantidad", "Estado")
            headers.forEach { table.addCell(it) }
            facturas.forEach {
                table.addCell(it.nombreAlumno)
                table.addCell(it.apellidosAlumno ?: "")
                table.addCell(it.whatsappAlumno ?: "")
                table.addCell(it.emailAlumno ?: "")
                table.addCell(it.fecha)
                table.addCell(it.cantidad.toString())
                table.addCell(if (it.pagada) "Pagada" else "Pendiente")
            }
            pdf.add(table)
            pdf.close()
            writer.close()
            compartirArchivo(file, "application/pdf")
        } catch (e: Exception) {
            Toast.makeText(context, "Error exportando PDF", Toast.LENGTH_SHORT).show()
        }
        exportando = false
    }

    private fun compartirArchivo(file: java.io.File, mime: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = mime
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(android.content.Intent.createChooser(intent, "Compartir archivo"))
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
    }
}

// Reutilizamos el data class Factura ya definido en el fragmento de profesor
