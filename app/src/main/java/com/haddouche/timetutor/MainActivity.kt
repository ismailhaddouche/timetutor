package com.haddouche.timetutor

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.haddouche.timetutor.navigation.AppNavHost
import com.haddouche.timetutor.ui.theme.TimeTutorTheme
import com.haddouche.timetutor.util.NotificationHelper
import com.haddouche.timetutor.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplico el tema guardado antes de super.onCreate()
        applyTheme()

        super.onCreate(savedInstanceState)

        // Inicializo Firebase
        initializeFirebase()
        
        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val navController = rememberNavController()
            val authViewModel: AuthViewModel = viewModel()

            TimeTutorTheme {
                AppNavHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    onThemeChange = { theme ->
                        saveThemePreference(theme)
                        recreate()
                    }
                )
            }
        }
    }

    // Aplico el tema que el usuario tenga guardado en SharedPreferences
    private fun applyTheme() {
        val prefs = getSharedPreferences("tema_app", Context.MODE_PRIVATE)
        when (prefs.getString("tema", "sistema")) {
            "claro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "oscuro" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // Guardo la preferencia de tema
    private fun saveThemePreference(theme: String) {
        getSharedPreferences("tema_app", Context.MODE_PRIVATE)
            .edit()
            .putString("tema", theme)
            .apply()
    }

    // Inicializo Firebase con persistencia offline y obtengo token FCM
    private fun initializeFirebase() {
        // Activo la persistencia offline para que funcione sin internet
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (_: Exception) {}

        // Pido el token de FCM para las notificaciones push
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token: $token")
            }
        }
    }
}
