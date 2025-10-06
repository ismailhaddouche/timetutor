package com.haddouche.timetutor.ui.profesor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.haddouche.timetutor.R
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConfiguracionProfesorFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var categorias = mutableListOf<Categoria>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuracion_profesor, container, false)
        val nombreCatField = view.findViewById<EditText>(R.id.editNombreCategoria)
        val precioCatField = view.findViewById<EditText>(R.id.editPrecioCategoria)
        val btnAddCat = view.findViewById<Button>(R.id.btnAddCategoria)
        val recyclerCat = view.findViewById<RecyclerView>(R.id.recyclerCategorias)
        recyclerCat.layoutManager = LinearLayoutManager(context)

        cargarCategorias {
            recyclerCat.adapter = CategoriaAdapter(categorias)
        }

        btnAddCat.setOnClickListener {
            val nombre = nombreCatField.text.toString().trim()
            val precio = precioCatField.text.toString().toDoubleOrNull() ?: 0.0
            if (nombre.isNotEmpty() && precio > 0) {
                val uidProfesor = auth.currentUser?.uid ?: ""
                val categoria = Categoria(nombre, precio)
                db.collection("categorias").document(uidProfesor + "_" + nombre).set(categoria)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Categoría añadida", Toast.LENGTH_SHORT).show()
                        cargarCategorias {
                            recyclerCat.adapter = CategoriaAdapter(categorias)
                        }
                    }
            } else {
                Toast.makeText(context, "Introduce nombre y precio válido", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun cargarCategorias(onLoaded: () -> Unit) {
        val uidProfesor = auth.currentUser?.uid ?: ""
        db.collection("categorias")
            .whereGreaterThanOrEqualTo("nombre", "")
            .get()
            .addOnSuccessListener { result ->
                categorias.clear()
                for (doc in result) {
                    val cat = doc.toObject(Categoria::class.java)
                    if (doc.id.startsWith(uidProfesor + "_")) {
                        categorias.add(cat)
                    }
                }
                onLoaded()
            }
    }
}

data class Categoria(val nombre: String = "", val precioMediaHora: Double = 0.0)

class CategoriaAdapter(private val categorias: List<Categoria>) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }
    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(categorias[position])
    }
    override fun getItemCount() = categorias.size

    class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(categoria: Categoria) {
            val nombreView = itemView.findViewById<TextView>(R.id.textNombreCategoria)
            val precioView = itemView.findViewById<TextView>(R.id.textPrecioCategoria)
            nombreView.text = categoria.nombre
            precioView.text = "Precio/30min: " + categoria.precioMediaHora + " €"
        }
    }
}
