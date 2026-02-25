package com.haddouche.timetutor.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.haddouche.timetutor.R
import com.haddouche.timetutor.data.AuthRepository
import com.haddouche.timetutor.data.FirestoreRepository
import com.haddouche.timetutor.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.tasks.Task as GmsTask

// Estado de la UI de autenticacion
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null,  // "profesor" o "alumno"
    val errorMessage: String? = null,
    val isCheckingAuth: Boolean = true,  // Para saber si estamos verificando el estado inicial
    val needsRoleSelection: Boolean = false // Indica si el usuario debe seleccionar rol (Google Sign-In primera vez)
)

// ViewModel para manejar la autenticacion
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AuthViewModel"
    private val db = FirestoreRepository

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Configuro el cliente de Google Sign-In con el Web Client ID que genera el plugin google-services
    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getApplication<Application>().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(getApplication<Application>(), gso)
    }

    init {
        checkCurrentUser()
    }

    // Verifico si hay un usuario ya logueado
    private fun checkCurrentUser() {
        val uid = AuthRepository.currentUid()
        if (uid.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(isCheckingAuth = true)
            db.getCollectionRef("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: "alumno"
                    _uiState.value = _uiState.value.copy(
                        isCheckingAuth = false,
                        isLoggedIn = true,
                        userRole = role
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error obteniendo rol del usuario actual", e)
                    _uiState.value = _uiState.value.copy(
                        isCheckingAuth = false,
                        isLoggedIn = false
                    )
                }
        } else {
            _uiState.value = _uiState.value.copy(isCheckingAuth = false)
        }
    }

    // Verifico si hay conexion a internet
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Login con email y password
    fun login(email: String, password: String, isStudent: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, introduce email y contraseña")
            return
        }
        if (!isNetworkAvailable()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión a Internet")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        AuthRepository.signInWithEmail(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        db.getCollectionRef("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                val userRole = document.getString("role") ?: if (isStudent) "alumno" else "profesor"
                                Log.d(TAG, "Login exitoso: ${user.uid}, rol: $userRole")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    userRole = userRole
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error obteniendo datos de usuario", e)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Error obteniendo datos del perfil"
                                )
                            }
                    }
                } else {
                    Log.w(TAG, "Error en login", task.exception)
                    val msg = parseLoginError(task.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
            }
    }

    // Registro de nuevo usuario
    fun register(email: String, password: String, isStudent: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor, introduce email y contraseña")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Introduce un email válido")
            return
        }
        if (!isNetworkAvailable()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión a Internet")
            return
        }

        val role = if (isStudent) "alumno" else "profesor"
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Primero verifico si el email ya existe
        db.getCollectionRef("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Este email ya está registrado"
                    )
                    return@addOnSuccessListener
                }
                // Si no existe, procedo con el registro
                proceedWithRegistration(email, password, role)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error verificando email existente, continuando con registro", e)
                proceedWithRegistration(email, password, role)
            }
    }

    private fun proceedWithRegistration(email: String, password: String, role: String) {
        AuthRepository.registerWithEmail(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        val userData = User(
                            uid = user.uid,
                            email = user.email ?: "",
                            role = role
                        )
                        db.getCollectionRef("users").document(user.uid).set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Usuario registrado exitosamente: ${user.uid}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    userRole = role
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error guardando datos de usuario", e)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Error guardando usuario"
                                )
                            }
                    }
                } else {
                    Log.w(TAG, "Error en registro", task.exception)
                    val msg = parseRegisterError(task.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = msg
                    )
                }
            }
    }

    // Recuperar contrasenya
    fun forgotPassword(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Introduce un email válido para recuperar la contraseña")
            return
        }

        AuthRepository.sendPasswordReset(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Correo de recuperación enviado")
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "No se pudo enviar el correo de recuperación")
                }
            }
    }

    // Proceso el resultado del intent de Google Sign-In
    // Ya no necesito isStudent porque el rol se decide después si es nuevo usuario
    fun handleGoogleSignInResult(task: GmsTask<GoogleSignInAccount>) {
        if (!isNetworkAvailable()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión a Internet")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudo obtener el token de Google"
                )
                return
            }

            // Uso el token para autenticarme en Firebase
            AuthRepository.signInWithGoogleCredential(idToken)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                            checkOrCreateGoogleUser(firebaseUser)
                        }
                    } else {
                        Log.w(TAG, "Error autenticando con Google en Firebase", authTask.exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error al iniciar sesión con Google"
                        )
                    }
                }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in falló con código: ${e.statusCode}", e)
            // Código 12501 = el usuario canceló, no muestro error
            val msg = if (e.statusCode == 12501) null
                else "Error de Google Sign-In (código ${e.statusCode})"
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = msg
            )
        }
    }

    // Si el usuario ya existe en Firestore obtengo su rol, si no, pido seleccion de rol
    private fun checkOrCreateGoogleUser(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        db.getCollectionRef("users").document(firebaseUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // El usuario ya existe, uso su rol guardado
                    val existingRole = document.getString("role") ?: "alumno"
                    Log.d(TAG, "Usuario Google ya existente: ${firebaseUser.uid}, rol: $existingRole")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = existingRole
                    )
                } else {
                    // Primera vez que entra con Google, necesito que seleccione rol
                    Log.d(TAG, "Usuario Google nuevo: ${firebaseUser.uid}, pidiendo rol")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsRoleSelection = true
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error verificando usuario Google en Firestore", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error verificando datos del usuario"
                )
            }
    }

    // Completo el registro de Google una vez seleccionado el rol
    fun completeGoogleRegistration(role: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)

        val userData = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            role = role,
            firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "",
            lastName = firebaseUser.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
            photoUrl = firebaseUser.photoUrl?.toString() ?: ""
        )

        db.getCollectionRef("users").document(firebaseUser.uid).set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Usuario Google creado: ${firebaseUser.uid}, rol: $role")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userRole = role,
                    needsRoleSelection = false
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creando usuario Google en Firestore", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error guardando datos del usuario"
                )
            }
    }

    // Logout - cierro sesión en Firebase y en Google
    fun logout() {
        AuthRepository.signOut()
        // También cierro sesión de Google para que la próxima vez pida elegir cuenta
        googleSignInClient.signOut()

        _uiState.value = AuthUiState(isLoggedIn = false, isCheckingAuth = false)
    }

    // Limpio el mensaje de error
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun parseLoginError(exception: Exception?): String {
        return when {
            exception?.message?.contains("no user record", true) == true ||
            exception?.message?.contains("user not found", true) == true ->
                "Usuario no registrado"
            exception?.message?.contains("password is invalid", true) == true ||
            exception?.message?.contains("wrong password", true) == true ->
                "Contraseña incorrecta"
            exception?.message?.contains("network", true) == true ->
                "Error de conexión. Comprueba tu Internet"
            exception?.message?.contains("too many requests", true) == true ->
                "Demasiados intentos. Espera unos minutos"
            else -> "Credenciales incorrectas o usuario no registrado"
        }
    }

    private fun parseRegisterError(exception: Exception?): String {
        return when {
            exception?.message?.contains("email address is already in use", true) == true ->
                "Este email ya está registrado"
            exception?.message?.contains("badly formatted", true) == true ->
                "Introduce un email válido"
            exception?.message?.contains("weak password", true) == true ||
            exception?.message?.contains("at least 6 characters", true) == true ->
                "La contraseña debe tener al menos 6 caracteres"
            exception?.message?.contains("network", true) == true ->
                "Error de conexión. Comprueba tu Internet"
            else -> "No se pudo registrar el usuario. Comprueba el email y la contraseña."
        }
    }
}
