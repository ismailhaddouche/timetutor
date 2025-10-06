package com.haddouche.timetutor.ui.profesor

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

class InicioProfesorFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio_profesor, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerClasesHoy)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val clasesHoy = getClasesHoyMock()
        recyclerView.adapter = ClaseAdapter(clasesHoy) { clase ->
            Toast.makeText(context, "Alumnos: " + clase.alumnos.joinToString(", ") { it.nombre }, Toast.LENGTH_LONG).show()
        }
        return view
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
data class ClaseHoy(val franja: String, val alumnos: List<Alumno>)
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
            franjaView.text = clase.franja
            alumnosView.text = "Alumnos: " + clase.alumnos.joinToString(", ") { it.nombre }
            itemView.setOnClickListener { onClick(clase) }
        }
    }
}
