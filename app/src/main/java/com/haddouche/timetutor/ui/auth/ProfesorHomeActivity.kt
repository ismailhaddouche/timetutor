package com.haddouche.timetutor.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.haddouche.timetutor.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast

class ProfesorHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica el tema guardado antes de mostrar la UI
        val prefs = getSharedPreferences("tema_app", Context.MODE_PRIVATE)
        when (prefs.getString("tema", "sistema")) {
            "claro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "oscuro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profesor_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavProfesor)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_inicio -> {
                    Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_perfil -> {
                    Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_alumnos -> {
                    Toast.makeText(this, "Alumnos", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_facturas -> {
                    Toast.makeText(this, "Facturas", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_configuracion -> {
                    Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}
