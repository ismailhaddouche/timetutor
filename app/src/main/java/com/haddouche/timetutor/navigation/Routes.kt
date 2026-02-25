package com.haddouche.timetutor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Rutas principales de navegacion
sealed class AppRoute(val route: String) {
    object Login : AppRoute("login")
    object TeacherHome : AppRoute("teacher_home")
    object StudentHome : AppRoute("student_home")
    object TeacherNotifications : AppRoute("teacher_notifications")
    object StudentNotifications : AppRoute("student_notifications")
}

// Items de navegacion inferior para profesor (5 tabs)
sealed class TeacherBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val index: Int
) {
    object Home : TeacherBottomNavItem("teacher_tab_home", "Inicio", Icons.Filled.Home, 0)
    object Profile : TeacherBottomNavItem("teacher_tab_profile", "Perfil", Icons.Filled.Person, 1)
    object Students : TeacherBottomNavItem("teacher_tab_students", "Alumnos", Icons.AutoMirrored.Filled.List, 2)
    object Invoices : TeacherBottomNavItem("teacher_tab_invoices", "Facturas", Icons.Filled.Info, 3)
    object Settings : TeacherBottomNavItem("teacher_tab_settings", "Config", Icons.Filled.Settings, 4)

    companion object {
        val items = listOf(Home, Profile, Students, Invoices, Settings)
    }
}

// Items de navegacion inferior para alumno (4 tabs)
sealed class StudentBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val index: Int
) {
    object Home : StudentBottomNavItem("student_tab_home", "Inicio", Icons.Filled.Home, 0)
    object Profile : StudentBottomNavItem("student_tab_profile", "Perfil", Icons.Filled.Person, 1)
    object Calendars : StudentBottomNavItem("student_tab_calendars", "Calendarios", Icons.Filled.DateRange, 2)
    object Settings : StudentBottomNavItem("student_tab_settings", "Config", Icons.Filled.Settings, 3)

    companion object {
        val items = listOf(Home, Profile, Calendars, Settings)
    }
}
