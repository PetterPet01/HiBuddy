package com.example.hibuddy.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.hibuddy.data.model.UserProfile
import com.example.hibuddy.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel : ViewModel() {

    var userProfile by mutableStateOf<UserProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    private val userRepository = UserRepository()

    fun loadCurrentUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            errorMessage = "User chưa đăng nhập"
            return
        }

        isLoading = true

        userRepository.getUserProfile(
            uid = uid,
            onSuccess = { profile ->
                userProfile = profile
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }
}