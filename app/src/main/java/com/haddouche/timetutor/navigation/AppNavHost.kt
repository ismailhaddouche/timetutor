package com.haddouche.timetutor.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.haddouche.timetutor.ui.auth.LoginScreen
import com.haddouche.timetutor.ui.alumno.StudentHomeScreen
import com.haddouche.timetutor.ui.common.NotificationsScreen
import com.haddouche.timetutor.ui.profesor.TeacherHomeScreen
import com.haddouche.timetutor.viewmodel.AuthViewModel
import com.haddouche.timetutor.viewmodel.NotificationsViewModel
import com.haddouche.timetutor.viewmodel.StudentHomeViewModel
import com.haddouche.timetutor.viewmodel.TeacherHomeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    onThemeChange: (String) -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()

    // Mientras verifico si hay usuario logueado, muestro loading
    if (authState.isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Reacciono a cambios en el estado de autenticacion para navegar
    LaunchedEffect(authState.isLoggedIn, authState.userRole) {
        if (authState.isLoggedIn && authState.userRole != null) {
            val destination = if (authState.userRole == "profesor") {
                AppRoute.TeacherHome.route
            } else {
                AppRoute.StudentHome.route
            }
            // Solo navego si no estoy ya en esa ruta
            if (navController.currentDestination?.route != destination) {
                navController.navigate(destination) {
                    popUpTo(AppRoute.Login.route) { inclusive = true }
                }
            }
        }
    }

    // Determino la ruta inicial
    val startDestination = when {
        authState.isLoggedIn && authState.userRole == "profesor" -> AppRoute.TeacherHome.route
        authState.isLoggedIn && authState.userRole == "alumno" -> AppRoute.StudentHome.route
        else -> AppRoute.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Ruta de Login
        composable(AppRoute.Login.route) {
            LoginScreen(
                authViewModel = authViewModel
            )
        }

        // Ruta de Home del Profesor
        composable(AppRoute.TeacherHome.route) {
            val teacherViewModel: TeacherHomeViewModel = viewModel()
            TeacherHomeScreen(
                viewModel = teacherViewModel,
                onShowNotifications = {
                    navController.navigate(AppRoute.TeacherNotifications.route)
                },
                onLogOut = {
                    authViewModel.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onThemeChange = onThemeChange
            )
        }

        // Ruta de Notificaciones del Profesor
        composable(AppRoute.TeacherNotifications.route) {
            val notificationsViewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Ruta de Home del Estudiante
        composable(AppRoute.StudentHome.route) {
            val studentViewModel: StudentHomeViewModel = viewModel()
            StudentHomeScreen(
                viewModel = studentViewModel,
                onShowNotifications = {
                    navController.navigate(AppRoute.StudentNotifications.route)
                },
                onLogOut = {
                    authViewModel.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onThemeChange = onThemeChange
            )
        }

        // Ruta de Notificaciones del Estudiante
        composable(AppRoute.StudentNotifications.route) {
            val notificationsViewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
