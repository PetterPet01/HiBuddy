package com.example.hibuddy.repository

import com.example.hibuddy.data.model.Project
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class ProjectRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createProject(
        project: Project,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("projects")
            .document(project.projectId)
            .set(project)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onFailure(it.message ?: "Create project failed")
            }
    }

    fun getProjects(
        currentUid: String,
        onSuccess: (List<Project>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("project_applications")
            .whereEqualTo("applicantId", currentUid)
            .get()
            .addOnSuccessListener { applicationResult ->

                val appliedProjectIds = applicationResult.documents
                    .mapNotNull { it.getString("projectId") }
                    .toSet()

                db.collection("projects")
                    .whereEqualTo("open", true)
                    .get()
                    .addOnSuccessListener { projectResult ->
                        val projects = projectResult.documents
                            .mapNotNull { it.toObject(Project::class.java) }
                            .filter { project ->
                                project.ownerId != currentUid &&
                                        project.projectId !in appliedProjectIds
                            }

                        onSuccess(projects)
                    }
                    .addOnFailureListener {
                        onFailure(it.message ?: "Load projects failed")
                    }
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Load applied projects failed")
            }
    }

    fun getMyProjects(
        currentUid: String,
        onSuccess: (List<Project>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("projects")
            .whereEqualTo("ownerId", currentUid)
            .get()
            .addOnSuccessListener { result ->
                val projects = result.documents.mapNotNull {
                    it.toObject(Project::class.java)
                }

                onSuccess(projects)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Load my projects failed")
            }
    }

    fun addMemberToProject(
        projectId: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("projects")
            .document(projectId)
            .update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(userId),
                    "currentMembers" to FieldValue.increment(1)
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Add member failed")
            }
    }

    fun getRelatedProjects(
        currentUid: String,
        onSuccess: (List<Project>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("projects")
            .get()
            .addOnSuccessListener { result ->

                val projects = result.documents
                    .mapNotNull {
                        it.toObject(Project::class.java)
                    }
                    .filter { project ->

                        project.ownerId == currentUid ||

                                project.memberIds.contains(currentUid)
                    }

                onSuccess(projects)
            }
            .addOnFailureListener {
                onFailure(it.message ?: "Load projects failed")
            }
    }
}