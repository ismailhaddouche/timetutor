package com.haddouche.timetutor.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.haddouche.timetutor.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast

class AlumnoHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alumno_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavAlumno)
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
                R.id.menu_calendarios -> {
                    Toast.makeText(this, "Calendarios", Toast.LENGTH_SHORT).show()
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
