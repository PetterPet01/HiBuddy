package com.example.hibuddy.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class MatchRepository {

    private val db = FirebaseFirestore.getInstance()

    fun likeUser(
        fromUserId: String,
        toUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val likeData = hashMapOf(
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "timestamp" to Timestamp.now()
        )

        db.collection("likes")
            .add(likeData)
            .addOnSuccessListener {
                checkMutualMatch(
                    fromUserId = fromUserId,
                    toUserId = toUserId,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Like failed")
            }
    }

    private fun checkMutualMatch(
        fromUserId: String,
        toUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("likes")
            .whereEqualTo("fromUserId", toUserId)
            .whereEqualTo("toUserId", fromUserId)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    createMatch(
                        user1Id = fromUserId,
                        user2Id = toUserId,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Check mutual match failed")
            }
    }

    private fun createMatch(
        user1Id: String,
        user2Id: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val matchId = listOf(user1Id, user2Id).sorted().joinToString("_")

        val matchData = hashMapOf(
            "matchId" to matchId,
            "users" to listOf(user1Id, user2Id),
            "createdAt" to Timestamp.now()
        )

        db.collection("matches")
            .document(matchId)
            .set(matchData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onFailure(it.message ?: "Create match failed")
            }
    }
}