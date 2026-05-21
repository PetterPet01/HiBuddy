package com.example.hibuddy.data.model

data class RoleOption(
    val role: String,
    val skills: List<String>
)

data class FieldOption(
    val field: String,
    val roles: List<RoleOption>
)

val projectOptions = listOf(

    FieldOption(
        field = "Information Technology",
        roles = listOf(
            RoleOption("Android Developer", listOf("Kotlin", "Jetpack Compose", "Firebase", "REST API", "Git")),
            RoleOption("iOS Developer", listOf("Swift", "SwiftUI", "Firebase", "API Integration")),
            RoleOption("Frontend Developer", listOf("HTML/CSS", "JavaScript", "React", "Vue", "UI Integration")),
            RoleOption("Backend Developer", listOf("Node.js", "Java", "Spring Boot", "SQL", "API Design")),
            RoleOption("Full-stack Developer", listOf("React", "Node.js", "Database", "Deployment")),
            RoleOption("Tester / QA", listOf("Test Case Writing", "Manual Testing", "Bug Reporting", "Automation Testing")),
            RoleOption("DevOps Engineer", listOf("Docker", "CI/CD", "Cloud", "Linux")),
            RoleOption("Cybersecurity Analyst", listOf("Network Security", "Penetration Testing", "Risk Assessment"))
        )
    ),

    FieldOption(
        field = "Artificial Intelligence & Data",
        roles = listOf(
            RoleOption("AI Engineer", listOf("Python", "Machine Learning", "Deep Learning", "Model Training")),
            RoleOption("Data Scientist", listOf("Python", "Statistics", "Machine Learning", "Data Visualization")),
            RoleOption("Data Analyst", listOf("Excel", "SQL", "Power BI", "Data Cleaning")),
            RoleOption("Data Engineer", listOf("SQL", "ETL", "Data Pipeline", "Cloud Database")),
            RoleOption("Prompt Engineer", listOf("Prompt Design", "LLM Evaluation", "AI Workflow"))
        )
    ),

    FieldOption(
        field = "Business & Startup",
        roles = listOf(
            RoleOption("Project Manager", listOf("Planning", "Team Coordination", "Task Management", "Communication")),
            RoleOption("Product Manager", listOf("Product Strategy", "User Research", "Roadmap Planning")),
            RoleOption("Business Analyst", listOf("Requirement Analysis", "Process Modeling", "Documentation")),
            RoleOption("Market Researcher", listOf("Survey Design", "Competitor Analysis", "Data Collection")),
            RoleOption("Operations Coordinator", listOf("Process Management", "Scheduling", "Problem Solving"))
        )
    ),

    FieldOption(
        field = "Marketing",
        roles = listOf(
            RoleOption("Marketing Specialist", listOf("Marketing Strategy", "Campaign Planning", "Market Research")),
            RoleOption("Content Creator", listOf("Content Writing", "Canva", "Storytelling", "Short Video")),
            RoleOption("Social Media Manager", listOf("Facebook", "TikTok", "Instagram", "Analytics")),
            RoleOption("SEO Specialist", listOf("Keyword Research", "SEO Writing", "Google Analytics")),
            RoleOption("Brand Strategist", listOf("Branding", "Positioning", "Customer Insight"))
        )
    ),

    FieldOption(
        field = "Design & Creative",
        roles = listOf(
            RoleOption("UI/UX Designer", listOf("Figma", "Wireframing", "Prototyping", "User Flow")),
            RoleOption("Graphic Designer", listOf("Canva", "Photoshop", "Illustrator", "Layout Design")),
            RoleOption("Video Editor", listOf("CapCut", "Premiere Pro", "Storyboarding")),
            RoleOption("Photographer", listOf("Photography", "Photo Editing", "Lighting")),
            RoleOption("Animator", listOf("Motion Design", "After Effects", "Animation"))
        )
    ),

    FieldOption(
        field = "Education",
        roles = listOf(
            RoleOption("Tutor", listOf("Teaching", "Presentation", "Lesson Planning")),
            RoleOption("Curriculum Designer", listOf("Learning Design", "Research", "Content Structuring")),
            RoleOption("Teaching Assistant", listOf("Communication", "Student Support", "Grading")),
            RoleOption("Education Content Creator", listOf("Script Writing", "Explaining Concepts", "Video Lessons"))
        )
    ),

    FieldOption(
        field = "Finance & Accounting",
        roles = listOf(
            RoleOption("Finance Planner", listOf("Budgeting", "Financial Analysis", "Excel")),
            RoleOption("Accountant", listOf("Bookkeeping", "Tax Basics", "Financial Reporting")),
            RoleOption("Investment Analyst", listOf("Market Analysis", "Risk Analysis", "Research")),
            RoleOption("Fundraising Coordinator", listOf("Pitch Deck", "Investor Research", "Communication"))
        )
    ),

    FieldOption(
        field = "Healthcare & Psychology",
        roles = listOf(
            RoleOption("Health Researcher", listOf("Research", "Survey Design", "Data Collection")),
            RoleOption("Medical Content Writer", listOf("Medical Writing", "Fact Checking", "Communication")),
            RoleOption("Mental Health Support Planner", listOf("Psychology Basics", "Empathy", "Community Support")),
            RoleOption("Healthcare Project Coordinator", listOf("Planning", "Documentation", "Team Coordination"))
        )
    ),

    FieldOption(
        field = "Environment & Social Impact",
        roles = listOf(
            RoleOption("Community Organizer", listOf("Community Management", "Event Planning", "Communication")),
            RoleOption("Environmental Researcher", listOf("Research", "Data Collection", "Report Writing")),
            RoleOption("Volunteer Coordinator", listOf("Volunteer Management", "Scheduling", "Leadership")),
            RoleOption("Sustainability Planner", listOf("Sustainability", "Impact Measurement", "Project Planning"))
        )
    ),

    FieldOption(
        field = "Language & Translation",
        roles = listOf(
            RoleOption("Translator", listOf("English", "Japanese", "Korean", "Chinese", "Localization")),
            RoleOption("Interpreter", listOf("Listening", "Speaking", "Note Taking")),
            RoleOption("Content Localizer", listOf("Translation", "Cultural Adaptation", "Editing")),
            RoleOption("Language Tutor", listOf("Teaching", "Grammar Explanation", "Speaking Practice"))
        )
    ),

    FieldOption(
        field = "Media & Communication",
        roles = listOf(
            RoleOption("PR Specialist", listOf("Public Relations", "Writing", "Communication Strategy")),
            RoleOption("Journalist", listOf("Interviewing", "Research", "News Writing")),
            RoleOption("Podcast Host", listOf("Speaking", "Interviewing", "Audio Editing")),
            RoleOption("Presenter", listOf("Public Speaking", "Storytelling", "Confidence"))
        )
    ),

    FieldOption(
        field = "Event & Tourism",
        roles = listOf(
            RoleOption("Event Organizer", listOf("Planning", "Logistics", "Budgeting")),
            RoleOption("Tour Planner", listOf("Travel Planning", "Customer Service", "Scheduling")),
            RoleOption("Hospitality Coordinator", listOf("Communication", "Service Mindset", "Problem Solving")),
            RoleOption("Sponsor Coordinator", listOf("Negotiation", "Proposal Writing", "Networking"))
        )
    ),

    FieldOption(
        field = "Engineering & Architecture",
        roles = listOf(
            RoleOption("Mechanical Designer", listOf("CAD", "Technical Drawing", "Problem Solving")),
            RoleOption("Electrical Engineer", listOf("Circuit Design", "IoT Basics", "Testing")),
            RoleOption("Architectural Designer", listOf("AutoCAD", "SketchUp", "Design Thinking")),
            RoleOption("Civil Project Assistant", listOf("Planning", "Documentation", "Site Coordination"))
        )
    ),

    FieldOption(
        field = "Law & Policy",
        roles = listOf(
            RoleOption("Legal Researcher", listOf("Legal Research", "Documentation", "Critical Thinking")),
            RoleOption("Policy Analyst", listOf("Policy Research", "Report Writing", "Data Analysis")),
            RoleOption("Compliance Assistant", listOf("Regulation Checking", "Documentation", "Attention to Detail"))
        )
    ),

    FieldOption(
        field = "Other",
        roles = listOf(
            RoleOption("Project Manager", listOf("Planning", "Communication", "Teamwork")),
            RoleOption("Researcher", listOf("Research", "Writing", "Analysis")),
            RoleOption("Designer", listOf("Creativity", "Visual Design", "Presentation")),
            RoleOption("Content Creator", listOf("Writing", "Storytelling", "Editing")),
            RoleOption("Volunteer", listOf("Teamwork", "Responsibility", "Communication"))
        )
    )
)