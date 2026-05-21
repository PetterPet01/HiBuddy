package com.example.hibuddy.ui.screens

import androidx.compose.ui.graphics.Color

// ── User / Contributor card ──────────────────────────────────────────────────

data class UserCard(
    val id: Int,

    val uid: String = "",
    val name: String,
    val username: String,
    val university: String,
    val bio: String,
    val roles: List<String>,          // max 3
    val skills: List<Skill>,
    val isVerified: Boolean,
    val avatarColor: Color,           // placeholder gradient base
    val avatarEmoji: String,
    val matchScore: Int,              // 0–100
    val projectsCompleted: Int,
    val reputationStars: Float,       // 0.0–5.0
    val location: String,
    val githubUrl: String? = null,
)

data class Skill(
    val name: String,
    val level: SkillLevel,
)

enum class SkillLevel(val label: String, val color: Color) {
    BEGINNER("Beginner", Color(0xFF4ECDC4)),
    INTERMEDIATE("Intermediate", Color(0xFF7C6AF7)),
    ADVANCED("Advanced", Color(0xFFFF6B6B)),
}

// ── Project card ─────────────────────────────────────────────────────────────

data class ProjectCard(
    val id: Int,
    val projectId: String,
    val title: String,
    val field: String,
    val description: String,
    val ownerName: String,
    val ownerEmoji: String,
    val ownerColor: Color,
    val rolesNeeded: List<RoleSlot>,
    val timeline: String,
    val workMode: String,             // Online / Offline / Hybrid
    val commitment: String,           // Casual / Serious / Fulltime
    val slotsTotal: Int,
    val slotsFilled: Int,
    val matchScore: Int,
    val accentColor: Color,
    val tags: List<String>,
)

data class RoleSlot(
    val role: String,
    val count: Int,
    val filled: Int,
    val skills: List<String>,
)

// ── Match ────────────────────────────────────────────────────────────────────

data class MatchItem(
    val id: Int,
    val name: String,
    val projectTitle: String,
    val role: String,
    val avatarEmoji: String,
    val avatarColor: Color,
    val lastMessage: String,
    val timeAgo: String,
    val isUnread: Boolean,
    val matchScore: Int,
    val hoursLeft: Int,               // Maximum of 72 hours
    val isNewMatch: Boolean = false,
    val isEstablished: Boolean = false,
    val isYourMove: Boolean = false
)

// ── Task ─────────────────────────────────────────────────────────────────────

data class TaskItem(
    val id: Int,
    val title: String,
    val projectTitle: String,
    val assignee: String,
    val priority: Priority,
    val deadline: String,
    val status: TaskStatus,
    val tag: String,
)

enum class Priority(val label: String, val color: Color) {
    LOW("Low", Color(0xFF4ECDC4)),
    MEDIUM("Medium", Color(0xFFFFD166)),
    HIGH("High", Color(0xFFFF8C42)),
    URGENT("Urgent", Color(0xFFFF4D6D)),
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE_REVIEW("Done · Review"),
    CLOSED("Closed"),
}

// ── My Projects ──────────────────────────────────────────────────────────────

data class MyProject(
    val id: Int,
    val title: String,
    val color: Color
)

// ── Sample Data ───────────────────────────────────────────────────────────────

object SampleData {

    val myProjects = listOf(
        MyProject(1, "EduMatch AI", Color(0xFF7C6AF7)),
        MyProject(2, "GreenTrace", Color(0xFF059669)),
        MyProject(3, "SoundMap", Color(0xFFE03055))
    )

    val users = listOf(
        UserCard(
            id = 1,
            name = "Nguyễn Minh Khoa",
            username = "@mkhoa.dev",
            university = "UIT — VNU HCM",
            bio = "Full-stack dev obsessed with clean architecture. Looking for a hardcore AI project to sink my teeth into.",
            roles = listOf("Backend Dev", "ML Engineer"),
            skills = listOf(
                Skill("Python", SkillLevel.ADVANCED),
                Skill("FastAPI", SkillLevel.INTERMEDIATE),
                Skill("PyTorch", SkillLevel.INTERMEDIATE),
            ),
            isVerified = true,
            avatarColor = Color(0xFF5B4FCF),
            avatarEmoji = "🧑‍💻",
            matchScore = 94,
            projectsCompleted = 7,
            reputationStars = 4.8f,
            location = "Ho Chi Minh City",
            githubUrl = "github.com/mkhoa-dev",
        ),
        UserCard(
            id = 2,
            name = "Trần Bảo Châu",
            username = "@bauchau.ui",
            university = "HCMUT — Bach Khoa",
            bio = "UI/UX designer who codes. Passionate about design systems and motion. Open to EdTech or HealthTech.",
            roles = listOf("UI/UX Designer", "Frontend Dev"),
            skills = listOf(
                Skill("Figma", SkillLevel.ADVANCED),
                Skill("Jetpack Compose", SkillLevel.INTERMEDIATE),
                Skill("After Effects", SkillLevel.BEGINNER),
            ),
            isVerified = true,
            avatarColor = Color(0xFFE03055),
            avatarEmoji = "🎨",
            matchScore = 87,
            projectsCompleted = 4,
            reputationStars = 4.5f,
            location = "Ho Chi Minh City",
        ),
        UserCard(
            id = 3,
            name = "Lê Hoàng Phúc",
            username = "@hphuc.data",
            university = "FPT University",
            bio = "Data scientist & competitive programmer. Seeking founding team for a recommendation engine startup.",
            roles = listOf("Data Scientist", "ML Engineer", "PM"),
            skills = listOf(
                Skill("SQL", SkillLevel.ADVANCED),
                Skill("Spark", SkillLevel.INTERMEDIATE),
                Skill("Kotlin", SkillLevel.BEGINNER),
            ),
            isVerified = false,
            avatarColor = Color(0xFF06B6D4),
            avatarEmoji = "📊",
            matchScore = 81,
            projectsCompleted = 3,
            reputationStars = 4.2f,
            location = "Hà Nội",
        ),
    )


    val matches = listOf(
        // Match Queue (Top Row - No messages yet)
        MatchItem(10, "Thu Hà", "EduMatch AI", "PM", "👩‍🚀", Color(0xFF7C6AF7), "", "", false, 91, 23, isNewMatch = true),
        MatchItem(11, "Đức Anh", "GreenTrace", "Android Dev", "🌿", Color(0xFF059669), "", "", false, 85, 45, isNewMatch = true),
        MatchItem(12, "Hoàng Phúc", "SoundMap", "Data Scientist", "📊", Color(0xFF06B6D4), "", "", false, 81, 12, isNewMatch = true),
        MatchItem(13, "Minh Tùng", "EduMatch AI", "Tester", "🤖", Color(0xFFFF8C42), "", "", false, 75, 68, isNewMatch = true),

        // Conversations - One Sided (Yellow Ring, Timer, "Your Move")
        MatchItem(1, "Nguyễn Minh Khoa", "GreenTrace", "Backend Dev", "🧑‍💻", Color(0xFF5B4FCF), "What's your favorite tech stack?", "1h ago", true, 94, 23, isNewMatch = false, isEstablished = false, isYourMove = true),
        MatchItem(2, "Trần Bảo Châu", "SoundMap", "UI Designer", "🎨", Color(0xFFE03055), "Hey how are you doing?", "2h ago", false, 87, 22, isNewMatch = false, isEstablished = false, isYourMove = true),
        MatchItem(3, "Lý Ngọc", "EduMatch AI", "QA", "🐞", Color(0xFFFF4D6D), "Hello :)", "5h ago", false, 88, 14, isNewMatch = false, isEstablished = false, isYourMove = true),

        // Conversations - Normal/Established (No ring, no timer)
        MatchItem(4, "Đặng Văn Nam", "GreenTrace", "Data Eng", "⚙️", Color(0xFF4CAF50), "Let's jump on a call tomorrow?", "1d ago", false, 82, 0, isNewMatch = false, isEstablished = true),
        MatchItem(5, "Lê Tuấn", "EduMatch AI", "DevOps", "🚀", Color(0xFF9C27B0), "I finished setting up the CI/CD pipeline.", "2d ago", false, 79, 0, isNewMatch = false, isEstablished = true),
        MatchItem(6, "Bích Ngọc", "SoundMap", "Frontend Dev", "💻", Color(0xFF00BCD4), "Great! Let's sync on Monday.", "3d ago", false, 90, 0, isNewMatch = false, isEstablished = true)
    )

    val tasks = listOf(
        TaskItem(1, "Design onboarding flow", "EduMatch AI", "You", Priority.HIGH, "Jun 15", TaskStatus.IN_PROGRESS, "Design"),
        TaskItem(2, "Set up FastAPI skeleton", "EduMatch AI", "Minh Khoa", Priority.URGENT, "Jun 12", TaskStatus.TODO, "Backend"),
        TaskItem(3, "Write matching algorithm v1", "EduMatch AI", "You", Priority.HIGH, "Jun 20", TaskStatus.TODO, "ML"),
        TaskItem(4, "Deploy to staging", "GreenTrace", "You", Priority.MEDIUM, "Jun 25", TaskStatus.DONE_REVIEW, "DevOps"),
        TaskItem(5, "Kafka pipeline setup", "GreenTrace", "Đức Anh", Priority.LOW, "Jul 1", TaskStatus.TODO, "Data"),
        TaskItem(6, "UI component library", "EduMatch AI", "Bảo Châu", Priority.MEDIUM, "Jun 18", TaskStatus.CLOSED, "Design"),
        TaskItem(7, "Finalize logo animation", "SoundMap", "Trần Bảo Châu", Priority.LOW, "Jul 5", TaskStatus.TODO, "Design"),
    )
}