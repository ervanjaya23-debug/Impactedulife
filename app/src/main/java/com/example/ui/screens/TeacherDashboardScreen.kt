package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.ColekAlarmScheduler
import com.example.data.NudgeRepository
import com.example.data.TaskRepository
import com.example.data.UserRepository
import com.example.data.local.LocalTaskRepository
import com.example.model.ColekTemplates
import com.example.model.Nudge
import com.example.model.NudgeType
import com.example.model.SubmissionStatus
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskScope
import com.example.model.TeacherSupervisionCalculator
import com.example.model.User
import com.example.model.toEntity
import com.example.ui.components.ColekAlarmVisualPreviewDialog
import com.example.ui.components.ColekDialog
import com.example.ui.components.ManageClassesDialog
import com.example.ui.components.ManageSchoolsDialog
import com.example.ui.components.ProfileSettingsDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.TeacherApproachingDeadlineAlertCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PrincipalTeal
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun TeacherDashboardScreen(
    currentUser: User,
    taskRepository: TaskRepository,
    nudgeRepository: NudgeRepository,
    userRepository: UserRepository,
    localTaskRepository: LocalTaskRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val alarmScheduler = remember { ColekAlarmScheduler(context) }

    var tabIndex by remember { mutableIntStateOf(0) }
    var showCreateStudentTaskDialog by remember { mutableStateOf(false) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var showManageSchoolsDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var selectedTaskForColek by remember { mutableStateOf<Task?>(null) }
    var previewAlarmTask by remember { mutableStateOf<Task?>(null) }

    val schoolMembersFlow = remember(currentUser.schoolId, userRepository) {
        userRepository.observeSchoolMembers(currentUser.schoolId)
    }
    val schoolMembers by schoolMembersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Teacher's managed schools & classes (synced with Firestore User profile)
    val managedSchools = remember {
        mutableStateListOf<String>().apply { addAll(currentUser.effectiveSchools) }
    }
    val managedClasses = remember {
        mutableStateListOf<String>().apply { addAll(currentUser.effectiveClasses) }
    }

    LaunchedEffect(currentUser.managedSchools, currentUser.schoolName) {
        val updated = currentUser.effectiveSchools
        if (updated != managedSchools.toList()) {
            managedSchools.clear()
            managedSchools.addAll(updated)
        }
    }

    LaunchedEffect(currentUser.managedClasses, currentUser.className) {
        val updated = currentUser.effectiveClasses
        if (updated != managedClasses.toList()) {
            managedClasses.clear()
            managedClasses.addAll(updated)
        }
    }

    var selectedSchoolFilter by remember { mutableStateOf("SEMUA SEKOLAH") }
    var selectedClassFilter by remember { mutableStateOf("SEMUA KELAS") }

    // Tab 0: Student tasks created by teachers
    val allStudentTasksFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolTasks(currentUser.schoolId, TaskScope.STUDENT_TASK.name)
    }
    val allStudentTasks by allStudentTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val filteredStudentTasks = remember(allStudentTasks, selectedClassFilter, selectedSchoolFilter) {
        allStudentTasks.filter { task ->
            val matchesClass = selectedClassFilter == "SEMUA KELAS" ||
                task.targetFilter.contains("SEMUA", ignoreCase = true) ||
                task.targetFilter.contains(selectedClassFilter, ignoreCase = true)
            val matchesSchool = selectedSchoolFilter == "SEMUA SEKOLAH" ||
                task.targetSchoolName.isBlank() ||
                task.targetSchoolName.contains("SEMUA", ignoreCase = true) ||
                task.targetSchoolName.contains(selectedSchoolFilter, ignoreCase = true)
            matchesClass && matchesSchool
        }
    }

    // Tab 1: Tasks assigned to teachers by the Principal (filtered to tasks targeting this teacher or all teachers)
    val principalTasksFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolTasks(currentUser.schoolId, TaskScope.TEACHER_TASK.name)
    }
    val allPrincipalTasks by principalTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val principalTasks = remember(allPrincipalTasks, currentUser, selectedSchoolFilter) {
        allPrincipalTasks.filter { task ->
            val assignedToMe = TeacherSupervisionCalculator.isTaskAssignedToTeacher(task, currentUser)
            val matchesSchool = selectedSchoolFilter == "SEMUA SEKOLAH" ||
                task.targetSchoolName.isBlank() ||
                task.targetSchoolName.contains("SEMUA", ignoreCase = true) ||
                task.targetSchoolName.contains(selectedSchoolFilter, ignoreCase = true)
            assignedToMe && matchesSchool
        }
    }

    val submissionsFlow = remember(currentUser.id, taskRepository) {
        taskRepository.observeUserSubmissions(currentUser.id)
    }
    val submissions by submissionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val allSchoolSubmissionsFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolSubmissions(currentUser.schoolId)
    }
    val allSchoolSubmissions by allSchoolSubmissionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val completedPrincipalTaskIds = remember(submissions) {
        submissions.filter { it.status == SubmissionStatus.COMPLETED.name }
            .map { it.taskId }
            .toSet()
    }

    // Approaching teacher tasks (uncompleted & within 48h of deadline)
    val approachingTeacherTasks = remember(principalTasks, completedPrincipalTaskIds) {
        val now = System.currentTimeMillis()
        principalTasks.filter { task ->
            task.id !in completedPrincipalTaskIds &&
                ColekTemplates.isApproachingDeadline(task.dueDate?.toDate()?.time ?: 0L, now)
        }
    }

    // Automatically schedule local alarms & fire timely local Colek notification for approaching tasks
    LaunchedEffect(principalTasks, completedPrincipalTaskIds) {
        if (principalTasks.isNotEmpty()) {
            alarmScheduler.syncAndNotifyApproachingTeacherTasks(
                teacherTasks = principalTasks,
                completedTaskIds = completedPrincipalTaskIds,
                teacherName = currentUser.displayName
            )
        }
    }

    val incomingNudgesFlow = remember(currentUser.id, nudgeRepository) {
        nudgeRepository.observeIncomingNudges(currentUser.id)
    }
    val incomingNudges by incomingNudgesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var dismissedNudgeId by remember { mutableStateOf<String?>(null) }
    var lastNotifiedNudgeId by remember { mutableStateOf<String?>(null) }
    val latestPrincipalNudge = remember(incomingNudges, dismissedNudgeId) {
        incomingNudges.lastOrNull { it.id != dismissedNudgeId }
    }

    // Trigger local notification with banner image when a new Nudge arrives for the teacher
    LaunchedEffect(latestPrincipalNudge?.id) {
        val nudge = latestPrincipalNudge
        if (nudge != null && nudge.id != lastNotifiedNudgeId) {
            lastNotifiedNudgeId = nudge.id
            val matchedTask = principalTasks.firstOrNull { it.id == nudge.taskId }
                ?: Task(
                    id = nudge.taskId,
                    title = nudge.taskTitle,
                    subjectOrCategory = "Supervisi Kepala Sekolah",
                    targetSchoolName = currentUser.schoolName,
                    targetFilter = currentUser.displayName,
                    scope = TaskScope.TEACHER_TASK.name
                )
            alarmScheduler.triggerImmediateColekNotification(
                task = matchedTask,
                recipientName = currentUser.displayName,
                customMessage = nudge.templateText
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Teacher Header Card with Profile Photo & School Name
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            UserAvatar(
                                photoUrl = currentUser.photoUrl,
                                displayName = currentUser.displayName,
                                size = 50.dp,
                                backgroundColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White,
                                fallbackIcon = Icons.Default.Assignment,
                                showEditBadge = true,
                                onClick = { showProfileSettingsDialog = true },
                                modifier = Modifier.testTag("teacher_header_avatar")
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = currentUser.schoolName.ifBlank { "Sekolah Terdaftar" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Panel Guru: ${currentUser.displayName} • ${currentUser.subjectSpecialty.ifBlank { "Mata Pelajaran" }}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${managedSchools.size} Sekolah • ${managedClasses.size} Kelas Diampu",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    previewAlarmTask = approachingTeacherTasks.firstOrNull()
                                        ?: principalTasks.firstOrNull()
                                        ?: Task(
                                            id = "sample_teacher_alarm",
                                            title = "Pengumpulan Modul Ajar & RPP",
                                            subjectOrCategory = "Modul Ajar & RPP",
                                            targetSchoolName = currentUser.schoolName.ifBlank { "SMA Negeri 1 Teladan" },
                                            targetFilter = currentUser.displayName,
                                            scope = TaskScope.TEACHER_TASK.name
                                        )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                    .testTag("teacher_alarm_preview_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Pratinjau Notifikasi Alarm Colek",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showAiAssistantDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                    .testTag("teacher_ai_assistant_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Asisten AI Gemini",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showProfileSettingsDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                    .testTag("teacher_edit_profile_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Atur Profil & Nama Sekolah",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Principal Nudge Alert Banner
                if (latestPrincipalNudge != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PrincipalTeal.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = PrincipalTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pesan Colek dari ${latestPrincipalNudge.fromUserName} (${latestPrincipalNudge.fromRole}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = latestPrincipalNudge.templateText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { dismissedNudgeId = latestPrincipalNudge.id }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-School Selector Bar for Teacher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sekolah Tempat Mengajar (${managedSchools.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { showManageSchoolsDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("teacher_manage_schools_btn")
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buat / Kelola Sekolah", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedSchoolFilter == "SEMUA SEKOLAH",
                            onClick = { selectedSchoolFilter = "SEMUA SEKOLAH" },
                            label = { Text("Semua Sekolah", fontSize = 11.sp) }
                        )
                    }
                    items(managedSchools) { school ->
                        FilterChip(
                            selected = selectedSchoolFilter == school,
                            onClick = { selectedSchoolFilter = school },
                            label = { Text("🏫 $school", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Tugas Siswa (${filteredStudentTasks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = {
                            val badge = if (approachingTeacherTasks.isNotEmpty()) " 🔔${approachingTeacherTasks.size}" else ""
                            Text("Tugas dari KS (${principalTasks.size})$badge", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Classes Filter Row (When in Tab 0: Tugas Siswa)
                if (tabIndex == 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kelas yang Diampu (${managedClasses.size} Kelas):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddClassDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("teacher_add_class_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buat / Kelola Kelas", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedClassFilter == "SEMUA KELAS",
                                onClick = { selectedClassFilter = "SEMUA KELAS" },
                                label = { Text("Semua Kelas (${allStudentTasks.size})", fontSize = 11.sp) }
                            )
                        }
                        items(managedClasses) { cls ->
                            val classTaskCount = allStudentTasks.count {
                                it.targetFilter.contains("SEMUA", ignoreCase = true) ||
                                    it.targetFilter.contains(cls, ignoreCase = true)
                            }
                            FilterChip(
                                selected = selectedClassFilter == cls,
                                onClick = { selectedClassFilter = cls },
                                label = { Text("$cls ($classTaskCount)", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Tab 1: Banner Notifikasi Lokal Colek & Gambar Alarm Menarik
                    TeacherApproachingDeadlineAlertCard(
                        approachingTasks = approachingTeacherTasks,
                        teacherName = currentUser.displayName,
                        schoolName = currentUser.schoolName,
                        alarmScheduler = alarmScheduler,
                        onOpenPreviewDialog = { task -> previewAlarmTask = task }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (tabIndex == 0) {
                // Student tasks created
                if (filteredStudentTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada tugas untuk filter kelas/sekolah ini. Tekan tombol + untuk memberi tugas ke kelas.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredStudentTasks, key = { it.id }) { task ->
                        val completedCount = allSchoolSubmissions.count {
                            it.taskId == task.id && it.status == SubmissionStatus.COMPLETED.name
                        }
                        Column {
                            TaskCard(
                                task = task,
                                isCompleted = false,
                                onToggleComplete = {},
                                onColekClicked = { selectedTaskForColek = task },
                                onToggleAlarm = { active ->
                                    if (active) {
                                        alarmScheduler.scheduleTaskAlarms(task)
                                        alarmScheduler.triggerImmediateColekNotification(
                                            task = task,
                                            recipientName = task.targetFilter.ifBlank { "Siswa" }
                                        )
                                        Toast.makeText(context, "Alarm & Notifikasi kelas aktif! ⏰", Toast.LENGTH_SHORT).show()
                                    } else {
                                        alarmScheduler.cancelTaskAlarms(task)
                                        Toast.makeText(context, "Alarm dinonaktifkan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 Progres Siswa: $completedCount selesai",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = buildString {
                                        if (task.targetSchoolName.isNotBlank()) append("🏫 ${task.targetSchoolName} • ")
                                        append("Kelas: ${task.targetFilter}")
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            } else {
                // Principal tasks assigned to teacher
                if (principalTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada tugas administrasi dari Kepala Sekolah.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(principalTasks, key = { it.id }) { task ->
                        val isDone = task.id in completedPrincipalTaskIds
                        TaskCard(
                            task = task,
                            isCompleted = isDone,
                            onToggleComplete = {
                                coroutineScope.launch {
                                    val dueMillis = task.dueDate?.toDate()?.time ?: Long.MAX_VALUE
                                    val isOntime = System.currentTimeMillis() <= dueMillis
                                    taskRepository.markTaskCompleted(
                                        taskId = task.id,
                                        userId = currentUser.id,
                                        userName = currentUser.displayName,
                                        schoolId = currentUser.schoolId,
                                        userRole = currentUser.role,
                                        isOntime = isOntime
                                    )
                                    alarmScheduler.cancelTaskAlarms(task)
                                    Toast.makeText(context, "Tugas administrasi ditandai selesai!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onColekClicked = null,
                            onToggleAlarm = { active ->
                                if (active) {
                                    alarmScheduler.scheduleTaskAlarms(task)
                                    alarmScheduler.triggerImmediateColekNotification(
                                        task = task,
                                        recipientName = currentUser.displayName
                                    )
                                    Toast.makeText(context, "Alarm & Notifikasi Colek KS aktif! ⏰", Toast.LENGTH_SHORT).show()
                                } else {
                                    alarmScheduler.cancelTaskAlarms(task)
                                    Toast.makeText(context, "Alarm dinonaktifkan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (tabIndex == 0) {
            FloatingActionButton(
                onClick = { showCreateStudentTaskDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("teacher_create_student_task_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Tugas Siswa")
            }
        }
    }

    // Dialog Kelola & Buat Kelas Berbeda (Multi-Kelas)
    if (showAddClassDialog) {
        ManageClassesDialog(
            managedClasses = managedClasses.toList(),
            onSaveClasses = { updatedClasses ->
                managedClasses.clear()
                managedClasses.addAll(updatedClasses)
                coroutineScope.launch {
                    userRepository.updateManagedClasses(
                        userId = currentUser.id,
                        managedClasses = updatedClasses
                    )
                    Toast.makeText(context, "Daftar ${updatedClasses.size} kelas berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddClassDialog = false }
        )
    }

    // Dialog Kelola & Buat Nama Sekolah (Multi-Sekolah untuk Guru)
    if (showManageSchoolsDialog) {
        ManageSchoolsDialog(
            managedSchools = managedSchools.toList(),
            activeSchoolName = currentUser.schoolName,
            onSaveSchools = { primarySchool, updatedSchools ->
                managedSchools.clear()
                managedSchools.addAll(updatedSchools)
                coroutineScope.launch {
                    userRepository.updateManagedSchools(
                        userId = currentUser.id,
                        schoolId = currentUser.schoolId,
                        activeSchoolName = primarySchool,
                        managedSchools = updatedSchools
                    )
                    Toast.makeText(context, "Daftar sekolah (${updatedSchools.size}) berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showManageSchoolsDialog = false }
        )
    }

    // Dialog Create Student Assignment (Supports Multiple Different Classes & Different Schools)
    if (showCreateStudentTaskDialog) {
        var title by remember { mutableStateOf("") }
        var classTargetMode by remember { mutableStateOf("PILIH KELAS") }
        val selectedClasses = remember {
            mutableStateListOf<String>().apply {
                val initial = if (selectedClassFilter != "SEMUA KELAS") selectedClassFilter
                else (managedClasses.firstOrNull() ?: "10-IPA-1")
                add(initial)
            }
        }
        var inlineNewClass by remember { mutableStateOf("") }

        var selectedTargetSchools by remember {
            mutableStateOf(
                listOf(
                    if (selectedSchoolFilter != "SEMUA SEKOLAH") selectedSchoolFilter
                    else currentUser.schoolName.ifBlank { managedSchools.firstOrNull() ?: "SMA Negeri 1 Teladan" }
                )
            )
        }
        var inlineNewSchool by remember { mutableStateOf("") }

        var subject by remember { mutableStateOf(currentUser.subjectSpecialty.ifBlank { "Matematika" }) }
        var description by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf(TaskPriority.HIGH) }
        var daysDeadline by remember { mutableIntStateOf(3) }
        var customDaysInput by remember { mutableStateOf("3") }

        val resolvedClassFilter = remember(classTargetMode, selectedClasses.toList()) {
            if (classTargetMode == "SEMUA KELAS" || selectedClasses.isEmpty()) {
                "SEMUA KELAS"
            } else {
                selectedClasses.joinToString(", ")
            }
        }

        val resolvedSchoolName = remember(selectedTargetSchools) {
            if (selectedTargetSchools.isEmpty()) currentUser.schoolName else selectedTargetSchools.joinToString(", ")
        }

        AlertDialog(
            onDismissRequest = { showCreateStudentTaskDialog = false },
            title = { Text("Beri Tugas ke Sekolah & Kelas", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Tugas (e.g. PR Bab 3 Trigonometri)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pilih Sekolah Tujuan (Multi-Sekolah)
                    Text("Pilih Sekolah Tujuan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val allSelected = selectedTargetSchools.size == managedSchools.size && managedSchools.size > 1
                            FilterChip(
                                selected = allSelected,
                                onClick = {
                                    selectedTargetSchools = if (allSelected) {
                                        listOf(managedSchools.firstOrNull().orEmpty())
                                    } else {
                                        managedSchools.toList()
                                    }
                                },
                                label = { Text("Semua Sekolah (${managedSchools.size})", fontSize = 11.sp) }
                            )
                        }
                        items(managedSchools) { school ->
                            val isSelected = school in selectedTargetSchools
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTargetSchools = if (isSelected && selectedTargetSchools.size > 1) {
                                        selectedTargetSchools - school
                                    } else {
                                        (selectedTargetSchools + school).distinct()
                                    }
                                },
                                label = { Text("🏫 $school", fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = inlineNewSchool,
                            onValueChange = { inlineNewSchool = it },
                            label = { Text("Tambah Sekolah Baru (Opsional)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                val clean = inlineNewSchool.trim()
                                if (clean.isNotBlank()) {
                                    if (clean !in managedSchools) managedSchools.add(clean)
                                    selectedTargetSchools = (selectedTargetSchools + clean).distinct()
                                    inlineNewSchool = ""
                                    coroutineScope.launch {
                                        userRepository.updateManagedSchools(
                                            userId = currentUser.id,
                                            schoolId = currentUser.schoolId,
                                            activeSchoolName = currentUser.schoolName.ifBlank { clean },
                                            managedSchools = managedSchools.toList()
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Sekolah", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pilih Kelas Target (Bisa Pilih Banyak Kelas Berbeda)
                    Text(
                        text = "Pilih Kelas Target (Bisa Pilih Beberapa Kelas Berbeda):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = classTargetMode == "SEMUA KELAS",
                            onClick = { classTargetMode = "SEMUA KELAS" },
                            label = { Text("Semua Kelas", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = classTargetMode == "PILIH KELAS",
                            onClick = { classTargetMode = "PILIH KELAS" },
                            label = { Text("Pilih Kelas (${selectedClasses.size})", fontSize = 11.sp) }
                        )
                    }

                    if (classTargetMode == "PILIH KELAS") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(managedClasses) { cls ->
                                        val isChosen = cls in selectedClasses
                                        FilterChip(
                                            selected = isChosen,
                                            onClick = {
                                                if (isChosen && selectedClasses.size > 1) {
                                                    selectedClasses.remove(cls)
                                                } else if (!isChosen) {
                                                    selectedClasses.add(cls)
                                                }
                                            },
                                            label = { Text(if (isChosen) "✓ $cls" else cls, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = inlineNewClass,
                                        onValueChange = { inlineNewClass = it.uppercase() },
                                        label = { Text("Buat Kelas Baru (e.g. 11-IPS-2)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val newEntries = inlineNewClass.split(",")
                                                .map { it.trim().uppercase() }
                                                .filter { it.isNotEmpty() }
                                            if (newEntries.isNotEmpty()) {
                                                newEntries.forEach { entry ->
                                                    if (entry !in managedClasses) managedClasses.add(entry)
                                                    if (entry !in selectedClasses) selectedClasses.add(entry)
                                                }
                                                inlineNewClass = ""
                                                coroutineScope.launch {
                                                    userRepository.updateManagedClasses(
                                                        userId = currentUser.id,
                                                        managedClasses = managedClasses.toList()
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+ Kelas", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Mata Pelajaran") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Instruksi / Catatan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Batas Waktu: $daysDeadline Hari lagi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(1, 2, 3, 5, 7, 14)) { d ->
                            FilterChip(
                                selected = daysDeadline == d,
                                onClick = {
                                    daysDeadline = d
                                    customDaysInput = d.toString()
                                },
                                label = { Text("$d Hari", fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customDaysInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            customDaysInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                daysDeadline = parsed.coerceIn(1, 365)
                            }
                        },
                        label = { Text("Kustom Batas Hari (1 - 365 Hari)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            coroutineScope.launch {
                                val safeDays = daysDeadline.coerceIn(1, 365)
                                val calendar = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, safeDays)
                                }
                                val newTask = Task(
                                    schoolId = currentUser.schoolId,
                                    createdBy = currentUser.id,
                                    creatorRole = currentUser.role,
                                    creatorName = currentUser.displayName,
                                    scope = TaskScope.STUDENT_TASK.name,
                                    title = title,
                                    subjectOrCategory = subject,
                                    targetFilter = resolvedClassFilter,
                                    targetSchoolName = resolvedSchoolName,
                                    description = description,
                                    priority = priority.name,
                                    dueDate = Timestamp(calendar.time)
                                )
                                val docId = taskRepository.createTask(newTask)
                                val savedTask = newTask.copy(id = docId)
                                localTaskRepository?.saveTask(savedTask.toEntity())
                                alarmScheduler.scheduleTaskAlarms(savedTask)
                                showCreateStudentTaskDialog = false
                                Toast.makeText(
                                    context,
                                    "Tugas & Alarm dibagikan ke $resolvedClassFilter ($resolvedSchoolName)! 📚",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Bagikan Tugas & Set Alarm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateStudentTaskDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Colek Santun Siswa
    selectedTaskForColek?.let { task ->
        val students = remember(schoolMembers, task.targetFilter) {
            schoolMembers.filter { student ->
                student.isStudent && (
                    task.targetFilter.isBlank() ||
                        task.targetFilter.contains("SEMUA", ignoreCase = true) ||
                        (student.className.isNotBlank() && task.targetFilter.contains(student.className, ignoreCase = true))
                    )
            }
        }
        ColekDialog(
            task = task,
            candidates = students,
            currentUserRole = currentUser.role,
            onSendNudge = { targetUser, templateText ->
                coroutineScope.launch {
                    val success = nudgeRepository.sendNudge(
                        Nudge(
                            schoolId = currentUser.schoolId,
                            fromUserId = currentUser.id,
                            fromUserName = currentUser.displayName,
                            fromRole = currentUser.role,
                            toUserId = targetUser.id,
                            toUserName = targetUser.displayName,
                            toRole = targetUser.role,
                            nudgeType = NudgeType.TEACHER_TO_STUDENT.name,
                            taskId = task.id,
                            taskTitle = task.title,
                            templateText = templateText
                        )
                    )
                    selectedTaskForColek = null
                    if (success) {
                        alarmScheduler.triggerImmediateColekNotification(
                            task = task,
                            recipientName = targetUser.displayName,
                            customMessage = templateText
                        )
                        Toast.makeText(context, "Colekan santun terkirim ke ananda ${targetUser.displayName}! 🌟", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Batas kuota harian tercapai untuk siswa ini", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { selectedTaskForColek = null },
            getRemainingQuota = { toUserId ->
                nudgeRepository.getRemainingNudgesToday(toUserId, task.id)
            }
        )
    }

    // Dialog Pratinjau Visual Notifikasi Alarm Colek Bergambar
    previewAlarmTask?.let { task ->
        ColekAlarmVisualPreviewDialog(
            task = task,
            recipientName = currentUser.displayName,
            alarmScheduler = alarmScheduler,
            onDismiss = { previewAlarmTask = null }
        )
    }

    if (showProfileSettingsDialog) {
        ProfileSettingsDialog(
            currentUser = currentUser,
            userRepository = userRepository,
            onDismiss = { showProfileSettingsDialog = false }
        )
    }

    if (showAiAssistantDialog) {
        com.example.ui.components.ColekAiAssistantDialog(
            currentUser = currentUser,
            taskRepository = taskRepository,
            localTaskRepository = localTaskRepository,
            onDismiss = { showAiAssistantDialog = false }
        )
    }
}
