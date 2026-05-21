package com.example.hibuddy.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.hibuddy.data.model.ProjectApplication
class ProjectApplicationRepository {

    private val db = FirebaseFirestore.getInstance()

    fun applyProject(
        projectId: String,
        applicantId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val applicationId = "${projectId}_$applicantId"

        val data = hashMapOf(
            "applicationId" to applicationId,
            "projectId" to projectId,
            "applicantId" to applicantId,
            "status" to "pending",
            "createdAt" to Timestamp.now()
        )

        db.collection("project_applications")
            .document(applicationId)
            .set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Apply project failed")
            }
    }

    fun getApplicationsByProject(
        projectId: String,
        onSuccess: (List<ProjectApplication>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("project_applications")
            .whereEqualTo("projectId", projectId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                val applications = result.documents.mapNotNull {
                    it.toObject(ProjectApplication::class.java)
                }

                onSuccess(applications)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Load applications failed")
            }
    }

    fun updateApplicationStatus(
        applicationId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("project_applications")
            .document(applicationId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onFailure(it.message ?: "Update application failed")
            }
    }

    fun approveApplication(
        applicationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        updateApplicationStatus(
            applicationId = applicationId,
            status = "approved",
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun rejectApplication(
        applicationId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        updateApplicationStatus(
            applicationId = applicationId,
            status = "rejected",
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun getApplicationsByApplicant(
        applicantId: String,
        onSuccess: (List<ProjectApplication>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("project_applications")
            .whereEqualTo("applicantId", applicantId)
            .get()
            .addOnSuccessListener { result ->
                val applications = result.documents.mapNotNull {
                    it.toObject(ProjectApplication::class.java)
                }

                onSuccess(applications)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Load applications failed")
            }
    }
}