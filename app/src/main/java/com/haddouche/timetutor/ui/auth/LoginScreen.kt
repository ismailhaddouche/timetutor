package com.haddouche.timetutor.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.haddouche.timetutor.R
import com.haddouche.timetutor.ui.theme.TimeTutorTheme
import com.haddouche.timetutor.viewmodel.AuthUiState
import com.haddouche.timetutor.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()

    // Preparo el launcher para el intent de Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        authViewModel.handleGoogleSignInResult(task)
    }

    // Diálogo de selección de rol para nuevos usuarios de Google
    if (uiState.needsRoleSelection) {
        AlertDialog(
            onDismissRequest = { /* No permitir cerrar sin elegir, o quizás logout */ },
            title = { Text("Bienvenido a TimeTutor") },
            text = { Text("Para completar tu registro, por favor selecciona si eres profesor o alumno.") },
            confirmButton = {
                Button(
                    onClick = { authViewModel.completeGoogleRegistration("profesor") }
                ) {
                    Text("Soy Profesor")
                }
            },
            dismissButton = {
                Button(
                    onClick = { authViewModel.completeGoogleRegistration("alumno") }
                ) {
                    Text("Soy Alumno")
                }
            }
        )
    }

    LoginScreenContent(
        uiState = uiState,
        onLoginClick = { email, password, isStudent ->
            authViewModel.login(email, password, isStudent)
        },
        onRegisterClick = { email, password, isStudent ->
            authViewModel.register(email, password, isStudent)
        },
        onForgotPasswordClick = { email ->
            authViewModel.forgotPassword(email)
        },
        onGoogleSignInClick = {
            val signInIntent = authViewModel.googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        },
        onClearError = { authViewModel.clearError() }
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    onLoginClick: (String, String, Boolean) -> Unit,
    onRegisterClick: (String, String, Boolean) -> Unit,
    onForgotPasswordClick: (String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isStudent by remember { mutableStateOf(true) }

    // Muestro el mensaje de error si hay
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isStudent,
                    onClick = { isStudent = true },
                    enabled = !uiState.isLoading
                )
                Text(
                    text = stringResource(R.string.auto_activity_login_15),
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = !isStudent,
                    onClick = { isStudent = false },
                    enabled = !uiState.isLoading
                )
                Text(
                    text = stringResource(R.string.auto_activity_login_16),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onLoginClick(email, password, isStudent) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.auto_activity_login_17))
                }
            }

            TextButton(
                onClick = { onForgotPasswordClick(email) },
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.auto_activity_login_18))
            }

            Button(
                onClick = { onRegisterClick(email, password, isStudent) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.auto_activity_login_19))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Separador visual entre los métodos de autenticación
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "o",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Google Sign-In
            OutlinedButton(
                onClick = { onGoogleSignInClick() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.auto_activity_login_44))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    TimeTutorTheme {
        LoginScreenContent(
            uiState = AuthUiState(),
            onLoginClick = { _, _, _ -> },
            onRegisterClick = { _, _, _ -> },
            onForgotPasswordClick = { },
            onGoogleSignInClick = { },
            onClearError = { }
        )
    }
}
