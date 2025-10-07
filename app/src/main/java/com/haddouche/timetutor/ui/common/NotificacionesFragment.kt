package com.haddouche.timetutor.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.haddouche.timetutor.R
import com.haddouche.timetutor.model.Notificacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView
import java.util.*

class NotificacionesFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var textSinNotificaciones: TextView
    private var notificaciones = listOf<Notificacion>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones, container, false)
        recyclerView = view.findViewById(R.id.recyclerNotificaciones)
        textSinNotificaciones = view.findViewById(R.id.textSinNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(context)
        cargarNotificaciones()
        return view
    }

    private fun cargarNotificaciones() {
        val uid = auth.currentUser?.uid ?: return
        val hace30dias = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        db.collection("notificaciones")
            .whereEqualTo("uidDestino", uid)
            .whereGreaterThanOrEqualTo("fecha", hace30dias)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                notificaciones = result.map { it.toObject(Notificacion::class.java) }
                recyclerView.adapter = NotificacionAdapter(notificaciones)
                textSinNotificaciones.visibility = if (notificaciones.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    class NotificacionAdapter(private val notificaciones: List<Notificacion>) : RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacion, parent, false)
            return NotificacionViewHolder(view)
        }
        override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
            holder.bind(notificaciones[position])
        }
        override fun getItemCount() = notificaciones.size

        class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(notificacion: Notificacion) {
                val tituloView = itemView.findViewById<TextView>(R.id.textTituloNoti)
                val mensajeView = itemView.findViewById<TextView>(R.id.textMensajeNoti)
                val fechaView = itemView.findViewById<TextView>(R.id.textFechaNoti)
                tituloView.text = notificacion.titulo
                mensajeView.text = notificacion.mensaje
                val fecha = Date(notificacion.fecha)
                fechaView.text = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", fecha)
            }
        }
    }
}
