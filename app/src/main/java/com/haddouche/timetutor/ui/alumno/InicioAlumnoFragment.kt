package com.haddouche.timetutor.ui.alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.haddouche.timetutor.R
import android.widget.TextView
import android.widget.Toast

class InicioAlumnoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio_alumno, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerClasesSemana)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val clasesSemana = getClasesSemanaMock()
        recyclerView.adapter = DiaAdapter(clasesSemana) { dia ->
            Toast.makeText(context, "Clases: " + dia.clases.joinToString(", ") { it.nombre }, Toast.LENGTH_LONG).show()
        }

        // Botón para acceder a facturas
        val btnFacturas = view.findViewById<View?>(R.id.btnFacturasAlumno)
        btnFacturas?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(id, com.haddouche.timetutor.ui.alumno.FacturasAlumnoFragment())
                .addToBackStack(null)
                .commit()
        }
        // Botón para acceder al centro de notificaciones
        val btnNotificaciones = view.findViewById<View?>(R.id.btnNotificaciones)
        btnNotificaciones?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(id, com.haddouche.timetutor.ui.common.NotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }
        return view
    }

    // Mock temporal para demo visual
    private fun getClasesSemanaMock(): List<DiaSemana> {
        return listOf(
            DiaSemana("Lunes", listOf(Clase("Matemáticas 16:00"), Clase("Inglés 18:00"))),
            DiaSemana("Martes", listOf(Clase("Física 17:00"))),
            DiaSemana("Miércoles", listOf()),
            DiaSemana("Jueves", listOf(Clase("Química 16:00"))),
            DiaSemana("Viernes", listOf(Clase("Historia 19:00"))),
            DiaSemana("Sábado", listOf()),
            DiaSemana("Domingo", listOf())
        )
    }
}

// Modelos para demo
data class DiaSemana(val dia: String, val clases: List<Clase>)
data class Clase(val nombre: String)

class DiaAdapter(
    private val dias: List<DiaSemana>,
    private val onClick: (DiaSemana) -> Unit
) : RecyclerView.Adapter<DiaAdapter.DiaViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dia_semana, parent, false)
        return DiaViewHolder(view)
    }
    override fun onBindViewHolder(holder: DiaViewHolder, position: Int) {
        holder.bind(dias[position], onClick)
    }
    override fun getItemCount() = dias.size

    class DiaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(dia: DiaSemana, onClick: (DiaSemana) -> Unit) {
            val diaView = itemView.findViewById<TextView>(R.id.textDia)
            val clasesView = itemView.findViewById<TextView>(R.id.textClases)
            diaView.text = dia.dia
            if (dia.clases.isEmpty()) {
                clasesView.text = "Sin clases"
            } else {
                clasesView.text = dia.clases.joinToString("\n") { it.nombre }
            }
            itemView.setOnClickListener { onClick(dia) }
        }
    }
}
