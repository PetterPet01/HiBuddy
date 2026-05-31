package com.example.hibuddy.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.AdminReportResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportManagementUiState(
    val isLoading: Boolean = false,
    val reports: List<AdminReportResponse> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class ReportManagementViewModel : ViewModel() {
    private val adminRepository = ServiceLocator.adminRepository

    private val _uiState = MutableStateFlow(ReportManagementUiState())
    val uiState = _uiState.asStateFlow()

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getReports().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, reports = it)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to load reports"
                    )
                }
            )
        }
    }

    fun dismissReport(reportId: String) {
        resolve(reportId, "DISMISS", "Report dismissed")
    }

    fun banReportedUser(reportId: String) {
        resolve(reportId, "BAN", "User banned")
    }

    private fun resolve(reportId: String, action: String, message: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, action).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = message,
                        reports = _uiState.value.reports.filter { report -> report.id != reportId }
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = it.message ?: "Failed to resolve report"
                    )
                }
            )
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReportManagementViewModel() as T
        }
    }
}