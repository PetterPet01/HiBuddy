package com.example.hibuddy.ui.common

object ProfileCatalog {
    val roleOptions = listOf(
        "Project Owner",
        "Project Manager",
        "Team Leader",
        "Business Analyst",
        "Researcher",
        "Frontend Developer",
        "Backend Developer",
        "Mobile Developer",
        "AI Engineer",
        "Data Analyst",
        "UI/UX Designer",
        "Graphic Designer",
        "Content Creator",
        "Copywriter",
        "Video Editor",
        "Photographer",
        "Marketing Specialist",
        "Social Media Manager",
        "Sales Representative",
        "Customer Support",
        "Event Planner",
        "Finance Planner",
        "Accountant",
        "Legal Advisor",
        "HR / Recruiter",
        "Teacher / Tutor",
        "Translator",
        "Community Manager",
        "Product Tester",
        "Volunteer Coordinator"
    ).sorted()

    val roleSkillMap = mapOf(
        "Frontend Developer" to listOf("HTML/CSS", "JavaScript", "React", "UI/UX Design", "GitHub"),
        "Backend Developer" to listOf("Backend Development", "SQL", "Python", "Java", "Node.js", "API Design"),
        "Mobile Developer" to listOf("Mobile Development", "Kotlin", "Jetpack Compose", "Firebase", "UI Design"),
        "AI Engineer" to listOf("Python", "Machine Learning", "Data Analysis", "Research"),
        "Data Analyst" to listOf("Data Analysis", "Excel", "SQL", "Python", "Research"),
        "UI/UX Designer" to listOf("Figma", "UI/UX Design", "User Interview", "Graphic Design"),
        "Project Manager" to listOf("Project Planning", "Task Management", "Leadership", "Communication")
    )
}