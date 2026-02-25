package com.haddouche.timetutor.data

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.Task

// Todo lo de autenticación lo pongo aquí para no llenar los Activities de código de Firebase
object AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun currentUid(): String = auth.currentUser?.uid ?: ""

    fun signInWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    fun registerWithEmail(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    // Inicio sesión con la credencial de Google usando el idToken del flujo de GoogleSignIn
    fun signInWithGoogleCredential(idToken: String): Task<AuthResult> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential)
    }

    fun sendPasswordReset(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }

    fun signOut() {
        auth.signOut()
    }
}
