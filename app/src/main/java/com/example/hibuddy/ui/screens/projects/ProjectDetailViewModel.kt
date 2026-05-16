package com.example.hibuddy.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val isLoading: Boolean = false,
    val project: ProjectResponse? = null,
    val tasks: List<TaskResponse> = emptyList(),
    val dashboard: DashboardResponse? = null,
    val error: String? = null
)

class ProjectDetailViewModel : ViewModel() {
    private val projectRepository = ServiceLocator.projectRepository
    private val taskRepository = ServiceLocator.taskRepository

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            projectRepository.getProject(projectId).fold(
                onSuccess = { project ->
                    _uiState.value = _uiState.value.copy(isLoading = false, project = project)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadTasks(projectId: String) {
        viewModelScope.launch {
            taskRepository.getTasks(projectId).fold(
                onSuccess = { tasks ->
                    _uiState.value = _uiState.value.copy(tasks = tasks)
                },
                onFailure = { }
            )
        }
    }

    fun loadDashboard(projectId: String) {
        viewModelScope.launch {
            taskRepository.getDashboard(projectId).fold(
                onSuccess = { dashboard ->
                    _uiState.value = _uiState.value.copy(dashboard = dashboard)
                },
                onFailure = { }
            )
        }
    }

    fun loadAll(projectId: String) {
        loadProject(projectId)
        loadTasks(projectId)
        loadDashboard(projectId)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    companion object {
        fun factory(projectId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val vm = ProjectDetailViewModel()
                    vm.loadAll(projectId)
                    return vm as T
                }
            }
        }
    }
}
