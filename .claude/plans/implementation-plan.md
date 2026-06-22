# HiBuddy - Complete Implementation Plan for 20 Issues
## 3-Stage Comprehensive Roadmap

Based on thorough codebase analysis, this plan addresses all 20 issues identified in the feedback document, organized into 3 implementation stages based on complexity, dependencies, and priority.

---

## **STAGE 1: Critical UI/UX Fixes & Foundation (Priority: High)**
**Estimated Duration: 1-2 weeks**
**Goal: Fix immediate user-facing issues and establish architectural foundations**

### **1.1 Notification System Enhancement (Issues #1)**

#### Backend Changes:
- **File**: `backend/app/api/chat.py`
  - The `/api/v1/notifications/unread-count` endpoint already exists (line 473-479)
  - Verify it's working correctly

#### Android Frontend Changes:
- **File**: `app/src/main/java/com/example/hibuddy/ui/components/NotificationBadge.kt` (NEW)
  - Create reusable `NotificationBadge` composable with:
    - Corner badge for bell icon with "!" or number
    - Red dot indicator for new notifications
    - Animation support
  
- **File**: `app/src/main/java/com/example/hibuddy/ui/navigation/TopAppBar.kt` or similar
  - Add badge overlay to notification icon
  - Poll `/api/v1/notifications/unread-count` or use WebSocket for real-time updates

#### Implementation Details:
```kotlin
@Composable
fun NotificationIconWithBadge(
    unreadCount: Int,
    onClick: () -> Unit
) {
    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") }
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
        }
    }
}
```

---

### **1.2 Chat "Last Seen" Bug Fix (Issue #2)**

#### Root Cause Analysis:
- **File**: `backend/app/api/websocket.py` (lines 87-122)
  - `presence_manager` handles user presence
- **File**: `backend/app/services/presence_service.py` (needs inspection)
  - Check if `last_seen_at` is updated when messages are sent

#### Backend Fix:
- **File**: `backend/app/api/websocket.py`
  - In `handle_websocket` function (line 124), when a message is sent (line 192-271):
    - Update sender's `last_seen_at` timestamp in presence_manager
    - Ensure timezone is UTC

- **File**: `backend/app/services/presence_service.py` (inspect and fix)
  - Add method: `update_last_activity(user_id: str)`
  - Call this whenever user sends a message or performs an action

#### Testing:
- Send messages between users
- Verify "Last seen" updates immediately
- Test timezone handling (UTC vs local time)

---

### **1.3 Avatar Overlapping Text Fix (Issue #3)**

#### Android Frontend Fix:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/discover/SwipeCard.kt` or similar
  - Likely in `UserCardResponse` rendering logic
  - Fix layout structure:

```kotlin
// BEFORE (likely broken):
Box {
    AsyncImage(url = avatarUrl) // Overlaps
    Text(text = bio) // Gets covered
}

// AFTER (fixed):
Column {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = avatarUrl,
            modifier = Modifier.size(64.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            Text(bio, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

---

### **1.4 Sidebar Text Formatting (Issue #4)**

#### Android Frontend Fix:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/projects/ProjectSidebar.kt` or navigation drawer
  - Increase font size from small (10-11sp) to medium (14sp)
  - Add proper line height (1.5x)
  - Increase sidebar width or add text wrapping

```kotlin
Text(
    text = projectDescription,
    style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    maxLines = 5,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(horizontal = 16.dp)
)
```

---

### **1.5 Language Consistency Resolution (Issue #5)**

#### Decision Required:
- **Clarification needed**: Is the app English-only or bilingual?
- Feedback shows:
  - Page 1: "Must be consistently English"
  - Page 11: "Change Feedback screen to Vietnamese"

#### Implementation (assuming bilingual with user preference):

**Backend:**
- **File**: `backend/app/models/user.py`
  - Add field: `preferred_language: Mapped[str] = mapped_column(String(5), default="en")`

**Android:**
- **File**: `app/src/main/res/values/strings.xml` (English)
- **File**: `app/src/main/res/values-vi/strings.xml` (Vietnamese)
- Use Android's built-in localization system
- Replace hardcoded strings with `stringResource(R.string.key)`

**Critical Files to Localize:**
- `FeedbackScreen.kt` → Full Vietnamese translation
- `AddSkillsByRole` component → Full English translation
- All UI strings → Use string resources

---

### **1.6 Project Timeframes (Issue #9 - Part A)**

#### Backend Changes:
- **File**: `backend/app/models/project.py` (lines 28-29)
  - ✅ Already has `start_date` and `end_date`!
  - No schema changes needed

#### Android Frontend Changes:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/projects/CreateProjectScreen.kt`
  - Verify date pickers are visible and functional
  - Ensure dates are sent in `DD/MM/YYYY` format (per backend validation line 58)

---

## **STAGE 2: Core Feature Improvements & Data Management (Priority: Medium-High)**
**Estimated Duration: 2-3 weeks**
**Goal: Implement missing features and improve search/matching algorithms**

### **2.1 Role & Skill Catalog Global Database (Issue #6)**

#### Current State:
- **File**: `backend/app/models/catalog.py`
  - ✅ `RoleCatalog` and `SkillCatalog` already exist!
  - ✅ Auto-creation logic exists in `backend/app/api/profile.py` (lines 203-214)

#### Enhancement Needed:
**Backend:**
- **File**: `backend/app/api/profile.py`
  - When user creates a role, **also create a UserRole** entry automatically
  - Add logic after line 214:

```python
# After creating RoleCatalog, also add it to user's roles
if not existing_role:
    user_role = UserRole(
        user_id=current_user.id,
        role_name=role_catalog.name,
        ordering=0,  # or next available
        catalog_role_id=role_catalog.id
    )
    db.add(user_role)
```

**Android:**
- **File**: Role selection dropdowns (inspect autocomplete components)
  - Fetch from `/api/v1/catalogs/roles` (needs new endpoint)
  - Show suggestions from global catalog

**New Backend Endpoint:**
```python
# backend/app/api/catalog.py (NEW FILE)
@router.get("/api/v1/catalogs/roles")
async def list_roles(db: AsyncSession = Depends(get_db)):
    roles = await db.execute(
        select(RoleCatalog).where(RoleCatalog.is_active == True)
        .order_by(RoleCatalog.name)
    )
    return [{"id": r.id, "name": r.name} for r in roles.scalars()]

@router.get("/api/v1/catalogs/skills")
async def list_skills(db: AsyncSession = Depends(get_db)):
    skills = await db.execute(
        select(SkillCatalog).where(SkillCatalog.is_active == True)
        .order_by(SkillCatalog.name)
    )
    return [{"id": s.id, "name": s.name} for s in skills.scalars()]
```

---

### **2.2 Swipe Card Missing Skill Requirements (Issue #7)**

#### Backend Enhancement:
- **File**: `backend/app/services/swipe_service.py` (inspect `get_discover_cards`)
  - Ensure `ProjectCardResponse` includes `role_slots` with `skill_requirements`
  - The schema likely exists in `backend/app/schemas/project.py`

- **File**: `backend/app/schemas/project.py`
  - Verify `ProjectCardResponse` includes:
```python
class ProjectCardResponse(BaseModel):
    project_id: UUID
    title: str
    description: str
    role_slots: list[RoleSlotResponse]  # Must include this
    # ... other fields
```

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/discover/ProjectDetailDialog.kt` (or similar)
  - When user clicks project card, show detailed modal with:
    - All role slots
    - Required skills per role
    - Skill levels (Beginner, Intermediate, Advanced)

```kotlin
LazyColumn {
    items(project.roleSlots) { slot ->
        RoleSlotCard(
            roleName = slot.roleName,
            openSlots = slot.count - slot.filled,
            skillRequirements = slot.skillRequirements
        )
    }
}
```

---

### **2.3 Flexible Search/Recommendation Algorithm (Issue #8)**

#### Current State:
- **File**: `backend/app/services/swipe_service.py` (needs inspection)
- **File**: `backend/app/milvus_client.py` (vector database for semantic search)
- **File**: `backend/app/services/embedding_service.py` (commented out in profile.py)

#### Implementation Strategy:

**Phase 1: Fuzzy Matching**
- **File**: `backend/app/services/search_service.py` (NEW or enhance existing)
  - Implement fuzzy string matching using `rapidfuzz` library
  - Add synonyms/aliases for common skills:
```python
SKILL_ALIASES = {
    "python": ["python3", "py", "python programming"],
    "javascript": ["js", "node", "nodejs", "react"],
    "machine learning": ["ml", "ai", "deep learning"],
    # ... more aliases
}
```

**Phase 2: Re-enable Vector Search**
- **File**: `backend/app/api/profile.py`
  - Uncomment lines 274, 289, 303, 349, 368, 419, 471
  - Re-enable `upsert_user_vector(profile)` calls
- **File**: `backend/app/api/project.py`
  - Uncomment lines 179, 252, 278, 426
  - Re-enable project vector embedding

**Phase 3: Hybrid Matching**
- Combine exact match + fuzzy match + semantic vector similarity
- Weight scores: 60% exact, 25% fuzzy, 15% semantic

---

### **2.4 Task Assignment to Project Owner (Issue #9 - Part B)**

#### Backend Changes:
- **File**: `backend/app/api/task.py` (lines 84-92)
  - Already allows owner as assignee! (line 84: `is_owner_assignee = assignee_uuid == project.owner_id`)
  - Verify this logic works end-to-end

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/tasks/CreateTaskScreen.kt`
  - In assignee dropdown, add "Project Owner" as first option
  - Fetch project owner from `project.ownerId`

```kotlin
val assigneeOptions = buildList {
    add(AssigneeOption(project.ownerId, "Project Owner (You)", isOwner = true))
    addAll(projectMembers.map { AssigneeOption(it.userId, it.displayName) })
}
```

---

### **2.5 Missing "Reject" Button for Tasks (Issue #10)**

#### Backend Enhancement:
- **File**: `backend/app/api/task.py`
  - Add new endpoint after line 367:

```python
@router.post("/tasks/{task_id}/reject")
async def reject_task(
    task_id: UUID,
    notes: str | None = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    
    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only owner can reject tasks")
    
    if task.status != "DONE_REVIEW":
        raise HTTPException(status_code=400, detail="Task must be in review")
    
    previous_status = task.status
    task.status = "TODO"  # or "REJECTED"
    task.checkout_at = None
    task.checkout_status = None
    
    db.add(TaskCheckoutHistory(
        task_id=task.id,
        action="REJECT",
        actor_id=current_user.id,
        previous_status=previous_status,
        new_status="TODO",
        notes=notes or "Owner rejected submission",
    ))
    
    # Notify assignee
    await notify_task_rejected(db, task, notes)
    
    return {"message": "Task rejected and returned to TODO"}
```

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/tasks/TaskReviewDialog.kt`
  - Add "Reject" button next to "Approve and Close"
  - Show text field for rejection reason

```kotlin
Row {
    OutlinedButton(onClick = { showRejectDialog = true }) {
        Text("Reject")
    }
    Spacer(modifier = Modifier.width(8.dp))
    Button(onClick = { approveTask() }) {
        Text("Approve and Close")
    }
}
```

---

### **2.6 Multi-User Task Assignment (Issue #11)**

#### Backend Schema Changes:
- **File**: `backend/app/models/task.py` (line 17)
  - **BREAKING CHANGE**: Replace `assignee_id` with many-to-many relationship

**New Approach:**
```python
# Create new table for task assignments
class TaskAssignment(Base):
    __tablename__ = "task_assignments"
    
    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    task_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("tasks.id", ondelete="CASCADE"), nullable=False
    )
    assignee_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    assigned_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
```

#### Migration Strategy:
1. Create `task_assignments` table
2. Migrate existing `task.assignee_id` → `task_assignments` rows
3. Update all task endpoints to handle multiple assignees
4. Update Android UI to show avatar group

#### Android Frontend:
- Display multiple avatars in a row
- Allow selecting multiple assignees in create/edit task

---

### **2.7 Student Verification Flow (Issue #14)**

#### Backend Enhancement:
- **File**: `backend/app/models/user.py` (lines 29-37)
  - ✅ Student verification fields already exist!

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/profile/CompleteProfileScreen.kt`
  - When `isStudent` checkbox is ticked, show modal:

```kotlin
if (isStudentTicked && !verificationPromptShown) {
    AlertDialog(
        onDismissRequest = { verificationPromptShown = true },
        title = { Text("Student Verification") },
        text = { 
            Text("Are you a student? Verify your student status to increase your prestige and unlock benefits.") 
        },
        confirmButton = {
            TextButton(onClick = { 
                navController.navigate("submit_student_verification")
            }) {
                Text("Verify Now")
            }
        },
        dismissButton = {
            TextButton(onClick = { verificationPromptShown = true }) {
                Text("Skip")
            }
        }
    )
}
```

- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/profile/SubmitStudentVerificationScreen.kt`
  - Already exists! Verify it's linked properly

---

## **STAGE 3: Advanced Features & Polish (Priority: Medium)**
**Estimated Duration: 2-3 weeks**
**Goal: Complete remaining features and enhance user experience**

### **3.1 Profile Detail Enhancements (Issue #15)**

#### Backend:
- **File**: `backend/app/api/profile.py` (lines 507-576)
  - ✅ `get_user_profile` already returns:
    - `project_history` (line 536-545)
    - `received_feedbacks` (line 546-556)
  - Verify these are non-empty

#### Enhancement Needed:
- **File**: Role skill requirements display
  - In `_role_responses` (lines 47-81), ensure skills are fully populated
  - Add skill level and requirements to response

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/profile/UserDetailScreen.kt`
  - Add tabs: "About" | "Projects" | "Feedback"
  - "Projects" tab: show `projectHistory` list
  - "Feedback" tab: show `receivedFeedbacks` with scores

```kotlin
TabRow(selectedTabIndex = selectedTab) {
    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
        Text("About")
    }
    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
        Text("Projects")
    }
    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
        Text("Feedback")
    }
}

when (selectedTab) {
    0 -> AboutTab(userDetail)
    1 -> ProjectHistoryTab(userDetail.projectHistory)
    2 -> FeedbackTab(userDetail.receivedFeedbacks)
}
```

---

### **3.2 Clickable Project History (Issue #16)**

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/profile/ProjectHistoryTab.kt`
  - Wrap project items with `clickable` modifier:

```kotlin
LazyColumn {
    items(projectHistory) { project ->
        ProjectHistoryItem(
            project = project,
            modifier = Modifier.clickable {
                navController.navigate("project_detail/${project.projectId}")
            }
        )
    }
}
```

---

### **3.3 Missing Feedback Score Display (Issue #17)**

#### Backend Verification:
- **File**: `backend/app/api/profile.py` (lines 547-555)
  - Check if `overall_score` is null for Lan Tran's profile
  - Verify `ProjectEvaluation` table has scores

#### Android Frontend:
- **File**: Feedback card component
  - Ensure score is displayed prominently:

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = feedback.overallScore.toString(),
        style = MaterialTheme.typography.headlineMedium,
        color = when {
            feedback.overallScore >= 4.0 -> Color.Green
            feedback.overallScore >= 3.0 -> Color.Orange
            else -> Color.Red
        }
    )
    Text(" / 5.0", style = MaterialTheme.typography.bodyMedium)
}
```

---

### **3.4 Separate "Close Project" and "Stop Recruiting" (Issue #12)**

#### Backend Enhancement:
- **File**: `backend/app/api/project.py`
  - Add new endpoint after line 337:

```python
@router.post("/{project_id}/stop-recruiting")
async def stop_recruiting(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=404, detail="Not authorized")
    
    if project.status == "RECRUITING":
        project.status = "ACTIVE"
        # Remove from swipe pool (delete_project_vector already called in add_member)
        return {"message": "Project is now active and no longer recruiting"}
    
    return {"message": "Project is not recruiting"}
```

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/projects/ProjectSettingsScreen.kt`
  - Add two separate buttons:

```kotlin
Column {
    if (project.status == "RECRUITING") {
        Button(onClick = { stopRecruiting() }) {
            Text("Stop Recruiting")
        }
        Text("Members can still join, but project won't appear in swipe pool")
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(
        onClick = { showCloseConfirmation = true },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("Close Project")
    }
    Text("This will close all tasks and end the project")
}
```

---

### **3.5 Remove "Pending Applicants" & Clarify "Open" Button (Issue #13)**

#### Android Frontend Changes:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/projects/ProjectDashboardScreen.kt` or similar
  - **Remove**: `PendingApplicantsSection` component entirely
  - **Rename**: "Open" button to "Open Workspace" or "View Details"

```kotlin
// DELETE THIS:
if (pendingApplicants.isNotEmpty()) {
    PendingApplicantsSection(applicants = pendingApplicants)
}

// CHANGE THIS:
Button(onClick = { /* ... */ }) {
    Text("Open")  // ❌ Unclear
}

// TO THIS:
Button(onClick = { /* ... */ }) {
    Icon(Icons.Default.Folder, contentDescription = null)
    Spacer(modifier = Modifier.width(4.dp))
    Text("Open Workspace")  // ✅ Clear
}
```

---

### **3.6 Real Names vs Display Names Clarification (Issue #18)**

#### Decision Required:
- Clarify with product owner: Should app show `username` or `full_name`?

#### Current State:
- **File**: `backend/app/models/user.py`
  - `username` (line 17): Unique login identifier
  - `full_name` (line 19): Display name
- **File**: `backend/app/models/profile.py`
  - `display_name`: Customizable display name (can differ from full_name)

#### Recommended Approach:
- Use `profile.display_name` for all public displays
- Use `user.username` only for login and @mentions
- Update all Android components to fetch and display `displayName`

---

### **3.7 Super Swipe Visual Effects & Limit Enforcement (Issue #19)**

#### Backend Verification:
- **File**: `backend/app/services/swipe_service.py` (inspect)
  - Check `get_daily_superlikes_remaining` function
  - Verify 7-day limit enforcement

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/discover/DiscoverScreen.kt`
  - Add Lottie animation or custom composable for super swipe:

```kotlin
if (isSuperSwiping) {
    Box(modifier = Modifier.fillMaxSize()) {
        LottieAnimation(
            composition = rememberLottieComposition(R.raw.super_swipe_animation),
            modifier = Modifier.align(Alignment.Center).size(200.dp)
        )
        // Or custom animation:
        AnimatedStarBurst(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
```

- Show "Super Swipes: X/3 remaining this week" indicator
- Disable button when limit reached with tooltip explanation

---

### **3.8 AI Course Suggestions Logic & Data Population (Issue #20)**

#### Backend Enhancement:
- **File**: `backend/app/services/suggestion_service.py` (inspect `generate_course_suggestions`)
  - Add conditional logic:

```python
async def generate_course_suggestions(db: AsyncSession, user_id: UUID):
    # Check for weaknesses from feedback
    weak_skills = await _get_user_weak_skills(db, user_id)
    
    # Check for incomplete tasks
    incomplete_tasks = await _get_incomplete_task_skills(db, user_id)
    
    # Only suggest if user has weaknesses or incomplete tasks
    if not weak_skills and not incomplete_tasks:
        return []
    
    # Generate suggestions based on weak points
    target_skills = list(set(weak_skills + incomplete_tasks))
    return await _fetch_courses_for_skills(db, target_skills)
```

#### Data Population:
- **File**: `backend/seed_courses.py` (NEW)
  - Populate `course_suggestions` table with real courses:
    - Coursera courses
    - Udemy courses
    - edX courses
    - YouTube playlists
  - Script to scrape or manually input 50+ courses per skill category

#### Android Frontend:
- **File**: `app/src/main/java/com/example/hibuddy/ui/screens/FeedbackScreen.kt`
  - Only show course suggestions section if `suggestions.isNotEmpty()`
  - Add "Why am I seeing this?" tooltip explaining trigger conditions

---

## **CROSS-CUTTING CONCERNS**

### **Testing Strategy**
For each stage:
1. **Unit Tests**: Backend endpoint tests
2. **Integration Tests**: Full user flows (swipe → match → chat → project join)
3. **UI Tests**: Compose UI tests for Android screens
4. **Manual Testing**: Real device testing for edge cases

### **Database Migrations**
- **Stage 1**: No schema changes
- **Stage 2**: 
  - Add `preferred_language` to `users` table
  - Create `task_assignments` table for multi-assignee support
- **Stage 3**: No schema changes

### **API Versioning**
- Keep `/api/v1/` prefix for all endpoints
- For breaking changes (multi-assignee tasks), create `/api/v2/tasks` endpoint

---

## **PRIORITY RECOMMENDATIONS**

### **Must Do (High Impact, High Priority)**
1. ✅ Issue #2: Chat "Last Seen" bug (affects user trust)
2. ✅ Issue #3: Avatar overlapping (visual bug)
3. ✅ Issue #6: Global role/skill catalog (core feature)
4. ✅ Issue #7: Show skill requirements on swipe (critical info)
5. ✅ Issue #10: Add "Reject" button for tasks (missing workflow)

### **Should Do (Medium Impact)**
6. Issue #8: Flexible search algorithm (improves matching quality)
7. Issue #11: Multi-user task assignment (team collaboration)
8. Issue #12: Separate close vs stop recruiting (workflow clarity)
9. Issue #14: Student verification flow (engagement feature)

### **Nice to Have (Low Impact or Polish)**
10. Issue #1: Notification badges (polish)
11. Issue #4: Sidebar text formatting (polish)
12. Issue #19: Super swipe visual effects (engagement polish)

---

## **ESTIMATED TOTAL EFFORT**
- **Stage 1**: 80-100 hours (1-2 weeks with 2 developers)
- **Stage 2**: 120-150 hours (2-3 weeks with 2 developers)
- **Stage 3**: 100-120 hours (2-3 weeks with 2 developers)
- **Total**: 300-370 hours (~6-8 weeks with 2 developers)

---

## **DEPENDENCIES & BLOCKERS**

### **Decisions Needed**
1. **Issue #5**: Language strategy - English-only or bilingual?
2. **Issue #18**: Display name strategy - username vs full_name vs display_name?

### **Technical Dependencies**
1. **Issue #8**: Requires Milvus vector database running for semantic search
2. **Issue #11**: Multi-assignee tasks require database migration (breaking change)
3. **Issue #20**: Requires course database population (data entry work)

---

## **SUCCESS METRICS**

### **Stage 1**
- [ ] All layout bugs fixed (visual inspection)
- [ ] "Last seen" shows accurate timestamps
- [ ] Notification badges display correctly

### **Stage 2**
- [ ] Users can create roles that appear in global catalog
- [ ] Swipe cards show all required skills
- [ ] Search returns fuzzy matches (test: "python" finds "py", "python3")
- [ ] Tasks can be rejected by project owners

### **Stage 3**
- [ ] User profiles show complete project history
- [ ] Course suggestions only appear for users with weaknesses
- [ ] Super swipe has visual effects and enforces weekly limit

---

## **NEXT STEPS**

1. **Get Stakeholder Approval**: Review this plan with product owner and team
2. **Resolve Open Questions**: Make decisions on language strategy and display names
3. **Set Up Environment**: Ensure Milvus, Redis, PostgreSQL are running
4. **Create Sprint Backlog**: Break down Stage 1 into 2-week sprint tasks
5. **Begin Implementation**: Start with highest priority fixes (Issues #2, #3, #10)

---

**Document Version**: 1.0  
**Created**: 2026-06-22  
**Author**: Development Planning  
**Status**: Ready for Review
