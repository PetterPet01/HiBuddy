package com.example.hibuddy.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = false,
    val projects: List<ProjectResponse> = emptyList(),
    val selectedProjectId: String? = null,
    val tasks: List<TaskResponse> = emptyList(),
    val dashboard: DashboardResponse? = null,
    val error: String? = null,
    val message: String? = null
)

class TasksViewModel : ViewModel() {

    private val taskRepository = ServiceLocator.taskRepository
    private val projectRepository = ServiceLocator.projectRepository

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            projectRepository.getMyProjects().fold(
                onSuccess = { projects ->
                    _uiState.value = _uiState.value.copy(isLoading = false, projects = projects, selectedProjectId = projects.firstOrNull()?.id)
                    projects.firstOrNull()?.id?.let { loadTasks(it) }
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun selectProject(projectId: String) {
        _uiState.value = _uiState.value.copy(selectedProjectId = projectId)
        loadTasks(projectId)
        loadDashboard(projectId)
    }

    fun loadTasks(projectId: String) {
        viewModelScope.launch {
            taskRepository.getTasks(projectId).fold(
                onSuccess = { tasks -> _uiState.value = _uiState.value.copy(tasks = tasks) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: String) {
        viewModelScope.launch {
            taskRepository.updateTaskStatus(taskId, newStatus).fold(
                onSuccess = {
                    val pid = _uiState.value.selectedProjectId
                    if (pid != null) loadTasks(pid)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun checkoutTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.checkoutTask(taskId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(message = "Checked out: ${response.checkoutStatus}")
                    val pid = _uiState.value.selectedProjectId
                    if (pid != null) loadTasks(pid)
                },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun loadDashboard(projectId: String) {
        viewModelScope.launch {
            taskRepository.getDashboard(projectId).fold(
                onSuccess = { dashboard -> _uiState.value = _uiState.value.copy(dashboard = dashboard) },
                onFailure = { }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TasksViewModel() as T
        }
    }
}
