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

    fun getUserProfile(
        uid: String,
        onSuccess: (UserProfile?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val userProfile = document.toObject(UserProfile::class.java)
                onSuccess(userProfile)
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "Load profile failed")
            }
    }

    fun getDiscoverUsers(
        currentUid: String,
        onSuccess: (List<UserProfile>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("likes")
            .whereEqualTo("fromUserId", currentUid)
            .get()
            .addOnSuccessListener { likeResult ->

                val likedUserIds = likeResult.documents
                    .mapNotNull { document ->
                        document.getString("toUserId")
                    }
                    .toSet()

                db.collection("users")
                    .get()
                    .addOnSuccessListener { userResult ->
                        val users = userResult.documents
                            .mapNotNull { document ->
                                document.toObject(UserProfile::class.java)
                            }
                            .filter { user ->
                                user.uid != currentUid && user.uid !in likedUserIds
                            }

                        onSuccess(users)
                    }
                    .addOnFailureListener { error ->
                        onFailure(error.message ?: "Load users failed")
                    }
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "Load liked users failed")
            }
    }
}