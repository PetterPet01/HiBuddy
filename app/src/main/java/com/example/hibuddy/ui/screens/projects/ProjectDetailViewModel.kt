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
    val isActionLoading: Boolean = false,
    val project: ProjectResponse? = null,
    val tasks: List<TaskResponse> = emptyList(),
    val dashboard: DashboardResponse? = null,
    val applicants: List<ApplicantResponse> = emptyList(),
    val currentUserId: String = ServiceLocator.authRepository.getUserId() ?: "",
    val error: String? = null,
    val message: String? = null
)

class ProjectDetailViewModel : ViewModel() {
    private val projectRepository = ServiceLocator.projectRepository
    private val taskRepository = ServiceLocator.taskRepository
    private val swipeRepository = ServiceLocator.swipeRepository

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            projectRepository.getProject(projectId).fold(
                onSuccess = { project ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        project = project,
                        applicants = if (project.ownerId == _uiState.value.currentUserId) _uiState.value.applicants else emptyList()
                    )
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

    fun loadApplicants(projectId: String) {
        viewModelScope.launch {
            swipeRepository.getApplicants(projectId).fold(
                onSuccess = { applicants ->
                    _uiState.value = _uiState.value.copy(applicants = applicants)
                },
                onFailure = { }
            )
        }
    }

    fun addMember(userId: String, role: String, roleSlotId: String? = null, matchId: String? = null) {
        val projectId = _uiState.value.project?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            projectRepository.addMember(projectId, userId, role, roleSlotId, matchId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, message = "Member added successfully")
                    loadAll(projectId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = e.message)
                }
            )
        }
    }

    fun closeProject() {
        val projectId = _uiState.value.project?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null)
            projectRepository.closeProject(projectId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, message = "Project closed")
                    loadAll(projectId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isActionLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadAll(projectId: String) {
        _uiState.value = _uiState.value.copy(applicants = emptyList())
        loadProject(projectId)
        loadTasks(projectId)
        loadDashboard(projectId)
        loadApplicants(projectId)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }

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
