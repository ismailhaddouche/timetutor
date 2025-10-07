package com.haddouche.timetutor.ui.auth

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.haddouche.timetutor.R
import com.haddouche.timetutor.model.User

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica el tema guardado antes de mostrar la UI
        val prefs = getSharedPreferences("tema_app", Context.MODE_PRIVATE)
        when (prefs.getString("tema", "sistema")) {
            "claro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "oscuro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val alumnoRadio = findViewById<RadioButton>(R.id.radioAlumno)
        val profesorRadio = findViewById<RadioButton>(R.id.radioProfesor)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val role = if (alumnoRadio.isChecked) "alumno" else "profesor"
            loginUser(email, password, role)
        }

        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val role = if (alumnoRadio.isChecked) "alumno" else "profesor"
            registerUser(email, password, role)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loginUser(email: String, password: String, role: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Por favor, introduce email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        db.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                val userRole = document.getString("role") ?: role
                                goToHome(userRole)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error obteniendo datos", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Credenciales incorrectas o usuario no registrado", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String, role: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Por favor, introduce email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show()
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = User(
                            uid = user.uid,
                            email = user.email ?: "",
                            role = role
                        )
                        db.collection("users").document(user.uid).set(userData)
                            .addOnSuccessListener {
                                goToHome(role)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error guardando usuario", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "No se pudo registrar el usuario. Comprueba el email y la contraseña.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToHome(role: String) {
        if (role == "profesor") {
            startActivity(Intent(this, ProfesorHomeActivity::class.java))
        } else {
            startActivity(Intent(this, AlumnoHomeActivity::class.java))
        }
        finish()
    }
}
