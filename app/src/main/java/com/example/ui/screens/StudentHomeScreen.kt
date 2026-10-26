package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.ColekAlarmScheduler
import com.example.data.DisciplineResult
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
import com.example.model.User
import com.example.ui.components.ColekDialog
import com.example.ui.components.DisciplineCelebrationDialog
import com.example.ui.components.DisciplineStreakCard
import com.example.ui.components.TaskCard
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CoralLate
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.OrangeFlame
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

enum class TaskStatusFilter(val label: String) {
    ALL("Semua"),
    UNCOMPLETED("Belum Selesai"),
    COMPLETED("Selesai"),
    LATE("Terlambat")
}

@Composable
fun StudentHomeScreen(
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
    var statusFilter by remember { mutableStateOf(TaskStatusFilter.ALL) }
    var showCreatePersonalDialog by remember { mutableStateOf(false) }
    var selectedTaskForColek by remember { mutableStateOf<Task?>(null) }
    var celebrationResult by remember { mutableStateOf<Pair<DisciplineResult, String>?>(null) }

    val classTasksFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolTasks(currentUser.schoolId, TaskScope.STUDENT_TASK.name)
    }
    val classTasks by classTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val personalTasksFlow = remember(currentUser.id, taskRepository) {
        taskRepository.observePersonalTasks(currentUser.id)
    }
    val personalTasks by personalTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val submissionsFlow = remember(currentUser.id, taskRepository) {
        taskRepository.observeUserSubmissions(currentUser.id)
    }
    val submissions by submissionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val schoolMembersFlow = remember(currentUser.schoolId, userRepository) {
        userRepository.observeSchoolMembers(currentUser.schoolId)
    }
    val schoolMembers by schoolMembersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val incomingNudgesFlow = remember(currentUser.id, nudgeRepository) {
        nudgeRepository.observeIncomingNudges(currentUser.id)
    }
    val incomingNudges by incomingNudgesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val completedTaskIds = remember(submissions) {
        submissions.filter { it.status == SubmissionStatus.COMPLETED.name || it.status == SubmissionStatus.LATE.name }
            .map { it.taskId }
            .toSet()
    }

    val allScopeTasks = remember(classTasks, personalTasks) {
        classTasks + personalTasks
    }

    val tasksInCurrentScope = remember(tabIndex, classTasks, personalTasks, allScopeTasks) {
        when (tabIndex) {
            1 -> classTasks
            2 -> personalTasks
            else -> allScopeTasks
        }
    }

    val taskStatusMap = remember(tasksInCurrentScope, submissions) {
        val now = System.currentTimeMillis()
        tasksInCurrentScope.associateWith { task ->
            val sub = submissions.firstOrNull { it.taskId == task.id }
            val isCompleted = sub != null && (sub.status == SubmissionStatus.COMPLETED.name || sub.status == SubmissionStatus.LATE.name)
            val isSubmittedLate = sub?.status == SubmissionStatus.LATE.name || (sub != null && !sub.isOntime)
            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
            val isOverdue = !isCompleted && dueMillis in 1 until now
            val isLate = isOverdue || isSubmittedLate
            Triple(isCompleted, isLate, isOverdue)
        }
    }

    val uncompletedCount = remember(taskStatusMap) {
        taskStatusMap.count { !it.value.first }
    }
    val completedCount = remember(taskStatusMap) {
        taskStatusMap.count { it.value.first }
    }
    val lateCount = remember(taskStatusMap) {
        taskStatusMap.count { it.value.second }
    }

    val displayedTasks = remember(tasksInCurrentScope, taskStatusMap, statusFilter) {
        when (statusFilter) {
            TaskStatusFilter.ALL -> tasksInCurrentScope
            TaskStatusFilter.UNCOMPLETED -> tasksInCurrentScope.filter { taskStatusMap[it]?.first == false }
            TaskStatusFilter.COMPLETED -> tasksInCurrentScope.filter { taskStatusMap[it]?.first == true }
            TaskStatusFilter.LATE -> tasksInCurrentScope.filter { taskStatusMap[it]?.second == true }
        }
    }

    // Latest unseen nudge banner
    var dismissedNudgeId by remember { mutableStateOf<String?>(null) }
    val latestNudge = remember(incomingNudges, dismissedNudgeId) {
        incomingNudges.lastOrNull { it.id != dismissedNudgeId }
    }

    val approachingTasksCount = remember(tasksInCurrentScope, taskStatusMap) {
        val now = System.currentTimeMillis()
        tasksInCurrentScope.count { task ->
            val isDone = taskStatusMap[task]?.first == true
            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
            !isDone && ColekTemplates.isApproachingDeadline(dueMillis, now)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Streak Kedisiplinan Card (Indikator Visual & Progress Bar Tingkat Disiplin)
                DisciplineStreakCard(user = currentUser)

                // Incoming Nudge Alert
                if (latestNudge != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangeFlame.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(OrangeFlame, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Colekan dari ${latestNudge.fromUserName}!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = latestNudge.templateText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { dismissedNudgeId = latestNudge.id }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Approaching Deadline Banner for Colek Santun
                if (approachingTasksCount > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("approaching_deadline_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangeFlame.copy(alpha = 0.12f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(OrangeFlame.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = OrangeFlame,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Colek Santun Deadline",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeFlame
                                )
                                Text(
                                    text = "Ada $approachingTasksCount tugas mendekati tenggat (<48 jam). Yuk saling semangati dengan Colek Santun!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scope Tabs: Semua, Tugas Kelas, Tugas Pribadi
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Semua (${allScopeTasks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Tugas Kelas (${classTasks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        text = { Text("Tugas Pribadi (${personalTasks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // UI Filter: Semua, Belum Selesai, Selesai, Terlambat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = statusFilter == TaskStatusFilter.ALL,
                        onClick = { statusFilter = TaskStatusFilter.ALL },
                        label = { Text("Semua (${tasksInCurrentScope.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("filter_chip_all")
                    )

                    FilterChip(
                        selected = statusFilter == TaskStatusFilter.UNCOMPLETED,
                        onClick = { statusFilter = TaskStatusFilter.UNCOMPLETED },
                        label = { Text("Belum Selesai ($uncompletedCount)", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueAccent.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("filter_chip_uncompleted")
                    )

                    FilterChip(
                        selected = statusFilter == TaskStatusFilter.COMPLETED,
                        onClick = { statusFilter = TaskStatusFilter.COMPLETED },
                        label = { Text("Selesai ($completedCount)", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldDone.copy(alpha = 0.18f),
                            selectedLabelColor = EmeraldDone,
                            selectedLeadingIconColor = EmeraldDone
                        ),
                        modifier = Modifier.testTag("filter_chip_completed")
                    )

                    FilterChip(
                        selected = statusFilter == TaskStatusFilter.LATE,
                        onClick = { statusFilter = TaskStatusFilter.LATE },
                        label = { Text("Terlambat ($lateCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralLate.copy(alpha = 0.2f),
                            selectedLabelColor = CoralLate,
                            selectedLeadingIconColor = CoralLate,
                            containerColor = if (lateCount > 0) CoralLate.copy(alpha = 0.08f) else Color.Transparent
                        ),
                        border = if (lateCount > 0 && statusFilter != TaskStatusFilter.LATE) {
                            BorderStroke(1.dp, CoralLate.copy(alpha = 0.5f))
                        } else null,
                        modifier = Modifier.testTag("filter_chip_late")
                    )
                }

                if (lateCount > 0 && statusFilter == TaskStatusFilter.UNCOMPLETED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overdue_warning_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CoralLate.copy(alpha = 0.12f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = CoralLate,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perhatian: Ada $lateCount tugas yang sudah lewat tenggat!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CoralLate,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { statusFilter = TaskStatusFilter.LATE },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralLate)
                            ) {
                                Text("Lihat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (displayedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val (emptyTitle, emptySubtitle) = when (statusFilter) {
                                TaskStatusFilter.LATE -> Pair(
                                    "Hebat! Tidak ada tugas terlambat! 🎉",
                                    "Semua tugas kamu berjalan dengan baik dan tepat waktu."
                                )
                                TaskStatusFilter.COMPLETED -> Pair(
                                    "Belum ada tugas selesai",
                                    "Tandai tugas yang selesai untuk menambah Streak Kedisiplinan!"
                                )
                                TaskStatusFilter.UNCOMPLETED -> Pair(
                                    "Tidak ada tugas tertunda! 🎉",
                                    "Bagus sekali! Waktunya istirahat atau buat catatan tugas pribadi."
                                )
                                TaskStatusFilter.ALL -> Pair(
                                    "Belum ada tugas",
                                    "Tugas dari guru atau catatan pribadi kamu akan muncul di sini."
                                )
                            }
                            Text(
                                text = emptyTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = emptySubtitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(displayedTasks, key = { it.id }) { task ->
                    val statusInfo = taskStatusMap[task]
                    val isDone = statusInfo?.first ?: (task.id in completedTaskIds)
                    val isLate = statusInfo?.second ?: false

                    TaskCard(
                        task = task,
                        isCompleted = isDone,
                        isLate = isLate,
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
                                val disciplineResult = userRepository.recordOnTimeTaskCompletion(
                                    userId = currentUser.id,
                                    isOntime = isOntime
                                )
                                localTaskRepository?.updateStatus(
                                    id = task.id,
                                    status = if (isOntime) "COMPLETED" else "LATE"
                                )
                                celebrationResult = Pair(disciplineResult, task.title)
                            }
                        },
                        onColekClicked = if (task.scope == TaskScope.STUDENT_TASK.name && !isDone) {
                            { selectedTaskForColek = task }
                        } else null,
                        onToggleAlarm = { active ->
                            if (active) {
                                alarmScheduler.scheduleTaskAlarms(task)
                                Toast.makeText(context, "Alarm Colek aktif! ⏰", Toast.LENGTH_SHORT).show()
                            } else {
                                alarmScheduler.cancelTaskAlarms(task)
                                Toast.makeText(context, "Alarm dinonaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Buat Tugas Pribadi
        FloatingActionButton(
            onClick = { showCreatePersonalDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_personal_task_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
        }
    }

    // Modal Colek Teman
    selectedTaskForColek?.let { task ->
        // Classmates in same class
        val classmates = schoolMembers.filter { it.id != currentUser.id && (it.className == currentUser.className || it.className.isBlank()) }
        ColekDialog(
            task = task,
            candidates = classmates,
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
                            nudgeType = NudgeType.PEER_STUDENT.name,
                            taskId = task.id,
                            taskTitle = task.title,
                            templateText = templateText
                        )
                    )
                    selectedTaskForColek = null
                    if (success) {
                        Toast.makeText(context, "Colekan berhasil terkirim ke ${targetUser.displayName}! 🚀", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Batas kuota harian tercapai untuk teman ini", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { selectedTaskForColek = null },
            getRemainingQuota = { toUserId ->
                nudgeRepository.getRemainingNudgesToday(toUserId, task.id)
            }
        )
    }

    // Modal Buat Tugas Pribadi
    if (showCreatePersonalDialog) {
        var personalTitle by remember { mutableStateOf("") }
        var personalSubject by remember { mutableStateOf("Catatan Pribadi") }
        var personalDesc by remember { mutableStateOf("") }
        var hoursFromNow by remember { mutableIntStateOf(3) }

        AlertDialog(
            onDismissRequest = { showCreatePersonalDialog = false },
            title = { Text("Buat Tugas Pribadi", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = personalTitle,
                        onValueChange = { personalTitle = it },
                        label = { Text("Judul Tugas (e.g. Baca Bab 4 Sejarah)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = personalSubject,
                        onValueChange = { personalSubject = it },
                        label = { Text("Mata Pelajaran / Topik") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = personalDesc,
                        onValueChange = { personalDesc = it },
                        label = { Text("Catatan Tambahan (opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tenggat Waktu Alarm:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = hoursFromNow == 3,
                            onClick = { hoursFromNow = 3 },
                            label = { Text("3 Jam") }
                        )
                        FilterChip(
                            selected = hoursFromNow == 6,
                            onClick = { hoursFromNow = 6 },
                            label = { Text("6 Jam") }
                        )
                        FilterChip(
                            selected = hoursFromNow == 24,
                            onClick = { hoursFromNow = 24 },
                            label = { Text("Besok") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (personalTitle.isNotBlank()) {
                            coroutineScope.launch {
                                val calendar = Calendar.getInstance().apply {
                                    add(Calendar.HOUR_OF_DAY, hoursFromNow)
                                }
                                val newTask = Task(
                                    schoolId = currentUser.schoolId,
                                    createdBy = currentUser.id,
                                    creatorRole = currentUser.role,
                                    creatorName = currentUser.displayName,
                                    scope = TaskScope.PERSONAL_TASK.name,
                                    title = personalTitle,
                                    subjectOrCategory = personalSubject,
                                    description = personalDesc,
                                    priority = TaskPriority.HIGH.name,
                                    dueDate = Timestamp(calendar.time)
                                )
                                val taskId = taskRepository.createTask(newTask)
                                alarmScheduler.scheduleTaskAlarms(newTask.copy(id = taskId))
                                showCreatePersonalDialog = false
                                Toast.makeText(context, "Tugas pribadi & alarm tersimpan! ⏰", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Simpan & Pasang Alarm")
                }
            },
            dismissButton = {
                Button(onClick = { showCreatePersonalDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Perayaan Streak Kedisiplinan
    celebrationResult?.let { (result, title) ->
        DisciplineCelebrationDialog(
            result = result,
            taskTitle = title,
            onDismiss = { celebrationResult = null }
        )
    }
}

