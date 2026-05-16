package com.example.hibuddy.repository

import com.example.hibuddy.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        userProfile: UserProfile,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .document(userProfile.uid)
            .set(userProfile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "Save profile failed")
            }
    }
}