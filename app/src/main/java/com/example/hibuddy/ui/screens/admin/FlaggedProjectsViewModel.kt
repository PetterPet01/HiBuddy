package com.example.hibuddy.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.ProjectResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlaggedProjectsUiState(
    val loading: Boolean = false,
    val projects: List<ProjectResponse> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class FlaggedProjectsViewModel : ViewModel() {
    private val repository = ServiceLocator.adminRepository
    private val _state = MutableStateFlow(FlaggedProjectsUiState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, message = null)
            repository.getFlaggedProjects().fold(
                onSuccess = { _state.value = FlaggedProjectsUiState(projects = it) },
                onFailure = {
                    _state.value = FlaggedProjectsUiState(error = it.message ?: "Failed to load projects")
                }
            )
        }
    }

    fun decide(projectId: String, approve: Boolean, reason: String) {
        viewModelScope.launch {
            val result = if (approve) {
                repository.approveProject(projectId, reason)
            } else {
                repository.rejectProject(projectId, reason)
            }
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        message = if (approve) "Project approved" else "Project rejected",
                        projects = _state.value.projects.filterNot { project -> project.id == projectId }
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(error = it.message)
                }
            )
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FlaggedProjectsViewModel() as T
        }
    }
}
