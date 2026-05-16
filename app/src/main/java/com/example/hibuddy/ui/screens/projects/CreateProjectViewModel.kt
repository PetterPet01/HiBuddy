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

data class CreateProjectUiState(
    val isLoading: Boolean = false,
    val createdProject: ProjectResponse? = null,
    val error: String? = null
)

class CreateProjectViewModel : ViewModel() {
    private val projectRepository = ServiceLocator.projectRepository

    private val _uiState = MutableStateFlow(CreateProjectUiState())
    val uiState: StateFlow<CreateProjectUiState> = _uiState.asStateFlow()

    fun createProject(
        title: String,
        field: String,
        description: String,
        specificGoal: String?,
        startDate: String,
        endDate: String,
        maxMembers: Int,
        workMode: String,
        commitmentLevel: String,
        roleSlots: List<RoleSlotRequest>,
        additionalRequirements: String?,
        memberBenefits: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            projectRepository.createProject(
                CreateProjectRequest(
                    title = title,
                    field = field,
                    description = description,
                    specificGoal = specificGoal,
                    startDate = startDate,
                    endDate = endDate,
                    maxMembers = maxMembers,
                    workMode = workMode,
                    commitmentLevel = commitmentLevel,
                    roleSlots = roleSlots,
                    additionalRequirements = additionalRequirements,
                    memberBenefits = memberBenefits
                )
            ).fold(
                onSuccess = { project ->
                    _uiState.value = _uiState.value.copy(isLoading = false, createdProject = project)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CreateProjectViewModel() as T
        }
    }
}
