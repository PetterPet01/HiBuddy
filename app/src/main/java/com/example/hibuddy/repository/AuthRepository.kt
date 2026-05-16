package com.example.hibuddy.repository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun register(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    onSuccess(uid)
                } else {
                    onFailure(task.exception?.message ?: "Register failed")
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }
}