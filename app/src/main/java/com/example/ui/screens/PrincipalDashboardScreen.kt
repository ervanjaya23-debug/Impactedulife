package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.ColekAlarmScheduler
import com.example.data.NudgeRepository
import com.example.data.TaskRepository
import com.example.data.UserRepository
import com.example.data.local.LocalTaskRepository
import com.example.model.ModulAjarProgressStatus
import com.example.model.Nudge
import com.example.model.NudgeType
import com.example.model.SubmissionStatus
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskScope
import com.example.model.TeacherSupervisionCalculator
import com.example.model.TeacherSupervisionSummary
import com.example.model.User
import com.example.model.UserRole
import com.example.model.toEntity
import com.example.ui.components.ColekAlarmVisualPreviewDialog
import com.example.ui.components.ColekDialog
import com.example.ui.components.ExportPdfDialog
import com.example.ui.components.ManageSchoolsDialog
import com.example.ui.components.ProfileSettingsDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CoralLate
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PrincipalTeal
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TeacherFilterOption(val label: String) {
    ALL("Semua Guru"),
    HIGH_PERFORMING("Disiplin Tepat Waktu (≥80%)"),
    NEEDS_SUPERVISION("Perlu Supervisi (<80%)"),
    MODUL_PENDING("Modul Belum Lengkap")
}

enum class ModulFilterOption(val label: String) {
    ALL("Semua Modul"),
    COMPLETED_ON_TIME("Selesai Tepat Waktu"),
    IN_PROGRESS("Sedang Dikerjakan"),
    OVERDUE("Terlambat")
}

@Composable
fun PrincipalDashboardScreen(
    currentUser: User,
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    nudgeRepository: NudgeRepository,
    localTaskRepository: LocalTaskRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val alarmScheduler = remember { ColekAlarmScheduler(context) }

    var selectedDashboardTab by remember { mutableIntStateOf(0) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showQuickModulDialog by remember { mutableStateOf(false) }
    var showExportPdfDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    var showManageSchoolsDialog by remember { mutableStateOf(false) }
    var previewAlarmTask by remember { mutableStateOf<Task?>(null) }

    var selectedTaskForColek by remember { mutableStateOf<Task?>(null) }
    var selectedTargetTeacher by remember { mutableStateOf<User?>(null) }

    // Principal's managed schools (multi-school support)
    val managedSchools = remember {
        mutableStateListOf<String>().apply { addAll(currentUser.effectiveSchools) }
    }
    LaunchedEffect(currentUser.managedSchools, currentUser.schoolName) {
        val updated = currentUser.effectiveSchools
        if (updated != managedSchools.toList()) {
            managedSchools.clear()
            managedSchools.addAll(updated)
        }
    }
    var selectedSchoolFilter by remember { mutableStateOf("SEMUA SEKOLAH") }

    // Search and filter state
    var teacherSearchQuery by remember { mutableStateOf("") }
    var selectedTeacherFilter by remember { mutableStateOf(TeacherFilterOption.ALL) }
    var selectedModulFilter by remember { mutableStateOf(ModulFilterOption.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf("Semua Kategori") }

    // Observe tasks for teachers (scope: TEACHER_TASK) - remembered to prevent recomposition flickering
    val teacherTasksFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolTasks(currentUser.schoolId, TaskScope.TEACHER_TASK.name)
    }
    val teacherTasks by teacherTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Observe all members to filter teachers and students
    val allMembersFlow = remember(currentUser.schoolId, userRepository) {
        userRepository.observeSchoolMembers(currentUser.schoolId)
    }
    val allMembers by allMembersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val teachers = remember(allMembers) {
        allMembers.filter { it.role == UserRole.TEACHER.name }
    }
    val students = remember(allMembers) {
        allMembers.filter { it.role == UserRole.STUDENT.name }
    }

    // Observe school submissions
    val submissionsFlow = remember(currentUser.schoolId, taskRepository) {
        taskRepository.observeSchoolSubmissions(currentUser.schoolId)
    }
    val submissions by submissionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Calculate individual teacher supervision summaries
    val now = remember { System.currentTimeMillis() }
    val teacherSummaries = remember(teachers, teacherTasks, submissions) {
        teachers.map { teacher ->
            TeacherSupervisionCalculator.calculateTeacherSummary(
                teacher = teacher,
                teacherTasks = teacherTasks,
                submissions = submissions,
                currentTimeMillis = now
            )
        }
    }

    // Calculate school overview
    val schoolOverview = remember(teachers, teacherSummaries) {
        TeacherSupervisionCalculator.calculateSchoolOverview(teachers, teacherSummaries)
    }

    // Filter teachers based on search query and filter chip
    val filteredTeacherSummaries = remember(teacherSummaries, teacherSearchQuery, selectedTeacherFilter) {
        teacherSummaries.filter { summary ->
            val matchesSearch = teacherSearchQuery.isBlank() ||
                summary.teacher.displayName.contains(teacherSearchQuery, ignoreCase = true) ||
                summary.teacher.subjectSpecialty.contains(teacherSearchQuery, ignoreCase = true) ||
                summary.teacher.email.contains(teacherSearchQuery, ignoreCase = true)

            val matchesFilter = when (selectedTeacherFilter) {
                TeacherFilterOption.ALL -> true
                TeacherFilterOption.HIGH_PERFORMING -> summary.onTimeRatePercent >= 80
                TeacherFilterOption.NEEDS_SUPERVISION -> summary.onTimeRatePercent < 80
                TeacherFilterOption.MODUL_PENDING -> summary.modulAjarStatus != ModulAjarProgressStatus.COMPLETED_ON_TIME
            }
            matchesSearch && matchesFilter
        }
    }

    // Modul ajar specific tasks
    val modulAjarTasks = remember(teacherTasks) {
        teacherTasks.filter {
            it.subjectOrCategory.contains("Modul", ignoreCase = true) ||
            it.title.contains("Modul", ignoreCase = true) ||
            it.subjectOrCategory.contains("RPP", ignoreCase = true)
        }
    }

    val filteredModulSummaries = remember(teacherSummaries, selectedModulFilter) {
        teacherSummaries.filter { summary ->
            when (selectedModulFilter) {
                ModulFilterOption.ALL -> true
                ModulFilterOption.COMPLETED_ON_TIME -> summary.modulAjarStatus == ModulAjarProgressStatus.COMPLETED_ON_TIME
                ModulFilterOption.IN_PROGRESS -> summary.modulAjarStatus == ModulAjarProgressStatus.PENDING
                ModulFilterOption.OVERDUE -> summary.modulAjarStatus == ModulAjarProgressStatus.OVERDUE || summary.modulAjarStatus == ModulAjarProgressStatus.COMPLETED_LATE
            }
        }
    }

    val filteredTasks = remember(teacherTasks, selectedCategoryFilter, selectedSchoolFilter) {
        teacherTasks.filter { task ->
            val matchesCat = selectedCategoryFilter == "Semua Kategori" ||
                task.subjectOrCategory.contains(selectedCategoryFilter, ignoreCase = true)
            val matchesSchool = selectedSchoolFilter == "SEMUA SEKOLAH" ||
                task.targetSchoolName.isBlank() ||
                task.targetSchoolName.contains("SEMUA", ignoreCase = true) ||
                task.targetSchoolName.contains(selectedSchoolFilter, ignoreCase = true)
            matchesCat && matchesSchool
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

                // Principal Header Banner
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PrincipalTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
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
                                    fallbackIcon = Icons.Default.School,
                                    showEditBadge = true,
                                    onClick = { showProfileSettingsDialog = true },
                                    modifier = Modifier.testTag("principal_header_avatar")
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentUser.schoolName.ifBlank { "Sekolah Terdaftar" },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Kepala Sekolah: ${currentUser.displayName.ifBlank { "Kepala Sekolah" }}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = "${managedSchools.size} Sekolah Binaan Aktif",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        previewAlarmTask = teacherTasks.firstOrNull() ?: Task(
                                            id = "sample_principal_alarm",
                                            title = "Pengumpulan Modul Ajar Semester Berjalan",
                                            subjectOrCategory = "Modul Ajar & RPP",
                                            targetSchoolName = currentUser.schoolName.ifBlank { "SMA Negeri 1 Teladan" },
                                            targetFilter = "SEMUA GURU",
                                            scope = TaskScope.TEACHER_TASK.name
                                        )
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                        .testTag("principal_alarm_preview_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Pratinjau Notifikasi Alarm Colek",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showProfileSettingsDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                        .testTag("principal_edit_profile_btn")
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

                        Spacer(modifier = Modifier.height(18.dp))

                        // Executive Supervision KPI Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${schoolOverview.averageOnTimeRatePercent}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketepatan Waktu",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Column {
                                Text(
                                    text = "${schoolOverview.totalOnTimeTasks}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tugas On-Time",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Column {
                                Text(
                                    text = "${schoolOverview.modulAjarCompliancePercent}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Modul Ajar",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Column {
                                Text(
                                    text = "${schoolOverview.totalTeachers}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "Dewan Guru",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual progress bar for overall school timeliness
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Kepatuhan Kedisiplinan Guru",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${schoolOverview.totalOnTimeTasks} on-time vs ${schoolOverview.totalLateOrPendingTasks} perlu diingatkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (schoolOverview.averageOnTimeRatePercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldDone,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multi-School Bar for Kepala Sekolah (Buat & Pilih Sekolah Berbeda)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sekolah / Unit Binaan (${managedSchools.size} Sekolah):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { showManageSchoolsDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("principal_manage_schools_btn")
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
                            label = { Text("Semua Sekolah (${teacherTasks.size})", fontSize = 11.sp) }
                        )
                    }
                    items(managedSchools) { school ->
                        val schoolTaskCount = teacherTasks.count {
                            it.targetSchoolName.isBlank() ||
                                it.targetSchoolName.contains("SEMUA", ignoreCase = true) ||
                                it.targetSchoolName.contains(school, ignoreCase = true)
                        }
                        FilterChip(
                            selected = selectedSchoolFilter == school,
                            onClick = { selectedSchoolFilter = school },
                            label = { Text("🏫 $school ($schoolTaskCount)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Tambah Anggota, Tugaskan Modul Cepat & Ekspor PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("principal_add_member_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Anggota", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showQuickModulDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("assign_new_modul_btn")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tugas Modul", fontSize = 11.sp, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { showExportPdfDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("header_export_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrincipalTeal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor PDF", fontSize = 11.sp, maxLines = 1, color = PrincipalTeal, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showAiAssistantDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("principal_ai_assistant_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Gemini", fontSize = 11.sp, maxLines = 1, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher for Principal Supervision
                TabRow(
                    selectedTabIndex = selectedDashboardTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedDashboardTab == 0,
                        onClick = { selectedDashboardTab = 0 },
                        modifier = Modifier.testTag("tab_supervision_summary"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Performa Guru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedDashboardTab == 1,
                        onClick = { selectedDashboardTab = 1 },
                        modifier = Modifier.testTag("tab_modul_ajar"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modul Ajar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedDashboardTab == 2,
                        onClick = { selectedDashboardTab = 2 },
                        modifier = Modifier.testTag("tab_admin_tasks"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tugas Guru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // -------------------------------------------------------------
            // TAB 0: RINGKASAN SUPERVISI PERFORMA GURU
            // -------------------------------------------------------------
            if (selectedDashboardTab == 0) {
                item {
                    // Search and Filter Bar
                    OutlinedTextField(
                        value = teacherSearchQuery,
                        onValueChange = { teacherSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_teacher_input"),
                        placeholder = { Text("Cari nama guru atau mata pelajaran...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (teacherSearchQuery.isNotBlank()) {
                                IconButton(onClick = { teacherSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips for teacher performance
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TeacherFilterOption.values()) { opt ->
                            FilterChip(
                                selected = selectedTeacherFilter == opt,
                                onClick = { selectedTeacherFilter = opt },
                                label = { Text(opt.label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_teacher_${opt.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ringkasan Performa Dewan Guru (${filteredTeacherSummaries.size} Guru)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pantau jumlah tugas tepat waktu, streak kedisiplinan, dan status modul ajar.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { showExportPdfDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Ekspor PDF",
                                tint = PrincipalTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 12.sp, color = PrincipalTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredTeacherSummaries.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (teacherSearchQuery.isNotBlank()) "Tidak ada guru yang cocok dengan pencarian" else "Belum ada dewan guru yang terdaftar.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (teachers.isEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showAddMemberDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal)
                                    ) {
                                        Text("Daftarkan Guru Pertama")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTeacherSummaries, key = { it.teacher.id }) { summary ->
                        TeacherPerformanceCard(
                            summary = summary,
                            teacherTasks = teacherTasks,
                            onColekClicked = { task ->
                                selectedTaskForColek = task
                                selectedTargetTeacher = summary.teacher
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 1: STATUS PENGERJAAN MODUL AJAR
            // -------------------------------------------------------------
            else if (selectedDashboardTab == 1) {
                item {
                    // Modul Ajar Executive Summary Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(IndigoPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = IndigoPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Supervisi Modul Ajar & RPP",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Kurikulum Merdeka • Semester Berjalan",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${schoolOverview.modulAjarCompliancePercent}% Tuntas",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (schoolOverview.modulAjarCompliancePercent >= 80) EmeraldDone else OrangeFlame
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Breakdown Pills
                            val onTimeCount = teacherSummaries.count { it.modulAjarStatus == ModulAjarProgressStatus.COMPLETED_ON_TIME }
                            val inProgressCount = teacherSummaries.count { it.modulAjarStatus == ModulAjarProgressStatus.PENDING }
                            val overdueCount = teacherSummaries.count {
                                it.modulAjarStatus == ModulAjarProgressStatus.OVERDUE || it.modulAjarStatus == ModulAjarProgressStatus.COMPLETED_LATE
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ModulStatusPill(
                                    label = "Tepat Waktu",
                                    count = onTimeCount,
                                    color = EmeraldDone,
                                    modifier = Modifier.weight(1f)
                                )
                                ModulStatusPill(
                                    label = "Dikerjakan",
                                    count = inProgressCount,
                                    color = IndigoPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                ModulStatusPill(
                                    label = "Terlambat",
                                    count = overdueCount,
                                    color = CoralLate,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter chips for Modul Ajar
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ModulFilterOption.values()) { opt ->
                            FilterChip(
                                selected = selectedModulFilter == opt,
                                onClick = { selectedModulFilter = opt },
                                label = { Text(opt.label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_modul_${opt.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Status Pengumpulan Modul Ajar Per Guru",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredModulSummaries.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Tidak ada guru pada filter status modul ini.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredModulSummaries, key = { "modul_${it.teacher.id}" }) { summary ->
                        ModulAjarTeacherCard(
                            summary = summary,
                            modulTasks = modulAjarTasks,
                            onColekTeacher = { task ->
                                selectedTaskForColek = task
                                selectedTargetTeacher = summary.teacher
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // -------------------------------------------------------------
            // TAB 2: KELOLA TUGAS ADMINISTRASI GURU
            // -------------------------------------------------------------
            else if (selectedDashboardTab == 2) {
                item {
                    val categories = listOf(
                        "Semua Kategori",
                        "Modul Ajar & RPP",
                        "Penginputan Nilai Raport & PTS",
                        "Laporan Absensi & Kehadiran",
                        "Administrasi Akreditasi",
                        "Supervisi Pembelajaran",
                        "Penyusunan Silabus"
                    )

                    Text(
                        text = "Kelola & Terbitkan Tugas Administrasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kepala Sekolah dapat memantau per kategori dan mencolek guru yang belum menyelesaikan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum ada tugas administrasi guru untuk kategori ini.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCreateTaskDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buat Tugas Administrasi Baru")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id }) { task ->
                        AdminTaskItemCard(
                            task = task,
                            teachers = teachers,
                            submissions = submissions,
                            onColekTeacher = { teacher ->
                                selectedTaskForColek = task
                                selectedTargetTeacher = teacher
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }

        // FAB Buat Tugas Guru
        FloatingActionButton(
            onClick = { showCreateTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("principal_add_task_fab"),
            containerColor = PrincipalTeal,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Buat Tugas Guru")
        }
    }

    // Modal Kirim Colek Guru
    if (selectedTaskForColek != null && selectedTargetTeacher != null) {
        val task = selectedTaskForColek!!
        val targetTeacher = selectedTargetTeacher!!
        ColekDialog(
            task = task,
            candidates = listOf(targetTeacher),
            currentUserRole = UserRole.PRINCIPAL.name,
            onSendNudge = { _, templateText ->
                coroutineScope.launch {
                    val success = nudgeRepository.sendNudge(
                        Nudge(
                            schoolId = currentUser.schoolId,
                            fromUserId = currentUser.id,
                            fromUserName = currentUser.displayName,
                            fromRole = currentUser.role,
                            toUserId = targetTeacher.id,
                            toUserName = targetTeacher.displayName,
                            toRole = targetTeacher.role,
                            nudgeType = NudgeType.PRINCIPAL_TO_TEACHER.name,
                            taskId = task.id,
                            taskTitle = task.title,
                            templateText = templateText
                        )
                    )
                    selectedTaskForColek = null
                    selectedTargetTeacher = null
                    if (success) {
                        alarmScheduler.triggerImmediateColekNotification(
                            task = task,
                            recipientName = targetTeacher.displayName,
                            customMessage = templateText
                        )
                        Toast.makeText(context, "Colekan ramah supervisi & notifikasi lokal terkirim ke ${targetTeacher.displayName}! 🙏", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Batas kuota harian tercapai untuk guru ini (maks 3x sehari)", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = {
                selectedTaskForColek = null
                selectedTargetTeacher = null
            },
            getRemainingQuota = { toUserId ->
                nudgeRepository.getRemainingNudgesToday(toUserId, task.id)
            }
        )
    }

    // Modal Tambah Guru / Siswa via Email
    if (showAddMemberDialog) {
        var emailInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var roleInput by remember { mutableStateOf(UserRole.TEACHER) }
        var specInput by remember { mutableStateOf("Matematika") }
        var classInput by remember { mutableStateOf("10-IPA-1") }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Daftarkan Anggota via Email", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Pengguna (Google)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Peran Anggota:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = roleInput == UserRole.TEACHER,
                            onClick = { roleInput = UserRole.TEACHER },
                            label = { Text("Guru") }
                        )
                        FilterChip(
                            selected = roleInput == UserRole.STUDENT,
                            onClick = { roleInput = UserRole.STUDENT },
                            label = { Text("Siswa") }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (roleInput == UserRole.TEACHER) {
                        OutlinedTextField(
                            value = specInput,
                            onValueChange = { specInput = it },
                            label = { Text("Mata Pelajaran (e.g. Matematika, Fisika)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = classInput,
                            onValueChange = { classInput = it },
                            label = { Text("Kelas Siswa (e.g. 10-IPA-1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && nameInput.isNotBlank()) {
                            coroutineScope.launch {
                                userRepository.addUserByEmail(
                                    email = emailInput,
                                    displayName = nameInput,
                                    role = roleInput.name,
                                    schoolId = currentUser.schoolId,
                                    schoolName = currentUser.schoolName,
                                    className = if (roleInput == UserRole.STUDENT) classInput else "",
                                    subjectSpecialty = if (roleInput == UserRole.TEACHER) specInput else ""
                                )
                                showAddMemberDialog = false
                                Toast.makeText(context, "$nameInput (${roleInput.name}) berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal)
                ) {
                    Text("Daftarkan Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddMemberDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Tugaskan Modul Cepat (Quick Modul Ajar Dialog)
    if (showQuickModulDialog) {
        var modulTitle by remember { mutableStateOf("Pengumpulan Modul Ajar Semester Berjalan") }
        var modulDesc by remember {
            mutableStateOf("Mohon melengkapi berkas Modul Ajar sesuai standar Kurikulum Merdeka beserta asesmen pembelajaran.")
        }
        var targetMode by remember { mutableStateOf("SEMUA GURU") }
        val selectedTeacherNames = remember { mutableStateListOf<String>() }
        var manualTeacherTarget by remember { mutableStateOf("") }
        val selectedTargetSchools = remember {
            mutableStateListOf<String>().apply {
                val initial = if (selectedSchoolFilter != "SEMUA SEKOLAH") selectedSchoolFilter
                else currentUser.schoolName.ifBlank { managedSchools.firstOrNull() ?: "SMA Negeri 1 Teladan" }
                add(initial)
            }
        }
        var daysDeadline by remember { mutableIntStateOf(7) }
        var customDaysInput by remember { mutableStateOf("7") }
        var isCustomDaysMode by remember { mutableStateOf(false) }

        val resolvedTargetFilter = remember(targetMode, selectedTeacherNames.toList(), manualTeacherTarget) {
            if (targetMode == "SEMUA GURU") {
                "SEMUA GURU"
            } else {
                val combined = buildList {
                    addAll(selectedTeacherNames)
                    if (manualTeacherTarget.isNotBlank()) {
                        add(manualTeacherTarget.trim())
                    }
                }
                if (combined.isEmpty()) "SEMUA GURU" else combined.joinToString(", ")
            }
        }

        val resolvedTargetSchool = remember(selectedTargetSchools.toList()) {
            if (selectedTargetSchools.isEmpty()) currentUser.schoolName.ifBlank { "SEMUA SEKOLAH" }
            else selectedTargetSchools.joinToString(", ")
        }

        AlertDialog(
            onDismissRequest = { showQuickModulDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = IndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tugaskan Modul Ajar Guru", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Tugaskan ke sekolah berbeda, seluruh dewan guru, atau pilih guru tertentu untuk mengumpulkan Modul Ajar & RPP.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = modulTitle,
                        onValueChange = { modulTitle = it },
                        label = { Text("Judul Modul Ajar") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modulDesc,
                        onValueChange = { modulDesc = it },
                        label = { Text("Instruksi & Kelengkapan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TeacherTargetAndDeadlineSelector(
                        managedSchools = managedSchools,
                        selectedTargetSchools = selectedTargetSchools,
                        onAddNewSchool = { newSchool ->
                            if (newSchool !in managedSchools) managedSchools.add(newSchool)
                            if (newSchool !in selectedTargetSchools) selectedTargetSchools.add(newSchool)
                            coroutineScope.launch {
                                userRepository.updateManagedSchools(
                                    userId = currentUser.id,
                                    schoolId = currentUser.schoolId,
                                    activeSchoolName = currentUser.schoolName.ifBlank { newSchool },
                                    managedSchools = managedSchools.toList()
                                )
                            }
                        },
                        teachers = teachers,
                        targetMode = targetMode,
                        onTargetModeChange = { targetMode = it },
                        selectedTeacherNames = selectedTeacherNames,
                        manualTeacherTarget = manualTeacherTarget,
                        onManualTeacherTargetChange = { manualTeacherTarget = it },
                        daysDeadline = daysDeadline,
                        onDaysDeadlineChange = { newDays ->
                            daysDeadline = newDays
                            customDaysInput = newDays.toString()
                        },
                        customDaysInput = customDaysInput,
                        onCustomDaysInputChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            customDaysInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                daysDeadline = parsed.coerceIn(1, 365)
                            }
                        },
                        isCustomDaysMode = isCustomDaysMode,
                        onCustomDaysModeChange = { isCustomDaysMode = it },
                        tagPrefix = "modul_dialog"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetMode == "PILIH GURU" && selectedTeacherNames.isEmpty() && manualTeacherTarget.isBlank()) {
                            Toast.makeText(context, "Pilih minimal 1 guru atau ketik nama guru tujuan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (modulTitle.isNotBlank()) {
                            coroutineScope.launch {
                                val safeDays = daysDeadline.coerceIn(1, 365)
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, safeDays) }
                                val newTask = Task(
                                    schoolId = currentUser.schoolId,
                                    createdBy = currentUser.id,
                                    creatorRole = UserRole.PRINCIPAL.name,
                                    creatorName = currentUser.displayName,
                                    scope = TaskScope.TEACHER_TASK.name,
                                    title = modulTitle,
                                    subjectOrCategory = "Modul Ajar & RPP",
                                    description = modulDesc,
                                    targetFilter = resolvedTargetFilter,
                                    targetSchoolName = resolvedTargetSchool,
                                    priority = TaskPriority.HIGH.name,
                                    dueDate = Timestamp(cal.time)
                                )
                                val docId = taskRepository.createTask(newTask)
                                val savedTask = newTask.copy(id = docId)
                                localTaskRepository?.saveTask(savedTask.toEntity())
                                alarmScheduler.scheduleTaskAlarms(savedTask)
                                showQuickModulDialog = false
                                Toast.makeText(
                                    context,
                                    "Modul Ajar ditugaskan ke $resolvedTargetFilter ($resolvedTargetSchool • $safeDays hari)! 📚",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Terbitkan Modul Ajar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuickModulDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Buat Tugas Administrasi dengan Kategori, Pilih Sekolah, Pilih Guru & Kustom Batas Hari
    if (showCreateTaskDialog) {
        val presetCategories = listOf(
            "Modul Ajar & RPP",
            "Penginputan Nilai Raport & PTS",
            "Laporan Absensi & Kehadiran",
            "Administrasi Akreditasi",
            "Supervisi Pembelajaran",
            "Penyusunan Silabus",
            "Lainnya"
        )
        var selectedCategory by remember { mutableStateOf(presetCategories[0]) }
        var customCategory by remember { mutableStateOf("") }
        var taskTitle by remember { mutableStateOf("Pengumpulan Modul Ajar Semester 1") }
        var taskDesc by remember { mutableStateOf("Mohon melengkapi berkas modul ajar sebelum tenggat waktu.") }
        var targetMode by remember { mutableStateOf("SEMUA GURU") }
        val selectedTeacherNames = remember { mutableStateListOf<String>() }
        var manualTeacherTarget by remember { mutableStateOf("") }
        val selectedTargetSchools = remember {
            mutableStateListOf<String>().apply {
                val initial = if (selectedSchoolFilter != "SEMUA SEKOLAH") selectedSchoolFilter
                else currentUser.schoolName.ifBlank { managedSchools.firstOrNull() ?: "SMA Negeri 1 Teladan" }
                add(initial)
            }
        }
        var daysDeadline by remember { mutableIntStateOf(5) }
        var customDaysInput by remember { mutableStateOf("5") }
        var isCustomDaysMode by remember { mutableStateOf(false) }

        val resolvedTargetFilter = remember(targetMode, selectedTeacherNames.toList(), manualTeacherTarget) {
            if (targetMode == "SEMUA GURU") {
                "SEMUA GURU"
            } else {
                val combined = buildList {
                    addAll(selectedTeacherNames)
                    if (manualTeacherTarget.isNotBlank()) {
                        add(manualTeacherTarget.trim())
                    }
                }
                if (combined.isEmpty()) "SEMUA GURU" else combined.joinToString(", ")
            }
        }

        val resolvedTargetSchool = remember(selectedTargetSchools.toList()) {
            if (selectedTargetSchools.isEmpty()) currentUser.schoolName.ifBlank { "SEMUA SEKOLAH" }
            else selectedTargetSchools.joinToString(", ")
        }

        AlertDialog(
            onDismissRequest = { showCreateTaskDialog = false },
            title = { Text("Tugaskan Administrasi Guru", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Pilih Kategori Tugas:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetCategories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    if (cat != "Lainnya") {
                                        taskTitle = when (cat) {
                                            "Modul Ajar & RPP" -> "Pengumpulan Modul Ajar Semester 1"
                                            "Penginputan Nilai Raport & PTS" -> "Upload Nilai Raport PTS"
                                            "Laporan Absensi & Kehadiran" -> "Laporan Rekap Absensi Bulanan"
                                            "Administrasi Akreditasi" -> "Kelengkapan Dokumen Akreditasi"
                                            "Supervisi Pembelajaran" -> "Jadwal Supervisi Mengajar"
                                            "Penyusunan Silabus" -> "Penyusunan Silabus & Capaian"
                                            else -> taskTitle
                                        }
                                    }
                                },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (selectedCategory == "Lainnya") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("Tulis Kategori Kustom") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Nama Tugas Administrasi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("Instruksi Kepala Sekolah") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TeacherTargetAndDeadlineSelector(
                        managedSchools = managedSchools,
                        selectedTargetSchools = selectedTargetSchools,
                        onAddNewSchool = { newSchool ->
                            if (newSchool !in managedSchools) managedSchools.add(newSchool)
                            if (newSchool !in selectedTargetSchools) selectedTargetSchools.add(newSchool)
                            coroutineScope.launch {
                                userRepository.updateManagedSchools(
                                    userId = currentUser.id,
                                    schoolId = currentUser.schoolId,
                                    activeSchoolName = currentUser.schoolName.ifBlank { newSchool },
                                    managedSchools = managedSchools.toList()
                                )
                            }
                        },
                        teachers = teachers,
                        targetMode = targetMode,
                        onTargetModeChange = { targetMode = it },
                        selectedTeacherNames = selectedTeacherNames,
                        manualTeacherTarget = manualTeacherTarget,
                        onManualTeacherTargetChange = { manualTeacherTarget = it },
                        daysDeadline = daysDeadline,
                        onDaysDeadlineChange = { newDays ->
                            daysDeadline = newDays
                            customDaysInput = newDays.toString()
                        },
                        customDaysInput = customDaysInput,
                        onCustomDaysInputChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            customDaysInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed > 0) {
                                daysDeadline = parsed.coerceIn(1, 365)
                            }
                        },
                        isCustomDaysMode = isCustomDaysMode,
                        onCustomDaysModeChange = { isCustomDaysMode = it },
                        tagPrefix = "admin_task_dialog"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetMode == "PILIH GURU" && selectedTeacherNames.isEmpty() && manualTeacherTarget.isBlank()) {
                            Toast.makeText(context, "Pilih minimal 1 guru atau ketik nama guru tujuan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (taskTitle.isNotBlank()) {
                            coroutineScope.launch {
                                val safeDays = daysDeadline.coerceIn(1, 365)
                                val calendar = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, safeDays)
                                }
                                val finalCategory = if (selectedCategory == "Lainnya" && customCategory.isNotBlank()) {
                                    customCategory
                                } else {
                                    selectedCategory
                                }
                                val newTask = Task(
                                    schoolId = currentUser.schoolId,
                                    createdBy = currentUser.id,
                                    creatorRole = UserRole.PRINCIPAL.name,
                                    creatorName = currentUser.displayName,
                                    scope = TaskScope.TEACHER_TASK.name,
                                    title = taskTitle,
                                    subjectOrCategory = finalCategory,
                                    description = taskDesc,
                                    targetFilter = resolvedTargetFilter,
                                    targetSchoolName = resolvedTargetSchool,
                                    priority = TaskPriority.HIGH.name,
                                    dueDate = Timestamp(calendar.time)
                                )
                                val docId = taskRepository.createTask(newTask)
                                val savedTask = newTask.copy(id = docId)
                                localTaskRepository?.saveTask(savedTask.toEntity())
                                alarmScheduler.scheduleTaskAlarms(savedTask)
                                showCreateTaskDialog = false
                                Toast.makeText(
                                    context,
                                    "Tugas guru ($finalCategory) diterbitkan ke $resolvedTargetFilter ($resolvedTargetSchool • $safeDays hari)! 🏫",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal)
                ) {
                    Text("Terbitkan Tugas Guru")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateTaskDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

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
                    Toast.makeText(context, "Daftar ${updatedSchools.size} sekolah berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showManageSchoolsDialog = false }
        )
    }

    previewAlarmTask?.let { task ->
        ColekAlarmVisualPreviewDialog(
            task = task,
            recipientName = task.targetFilter.ifBlank { "Dewan Guru" },
            alarmScheduler = alarmScheduler,
            onDismiss = { previewAlarmTask = null }
        )
    }

    if (showExportPdfDialog) {
        ExportPdfDialog(
            currentUser = currentUser,
            schoolOverview = schoolOverview,
            allSummaries = teacherSummaries,
            filteredSummaries = filteredTeacherSummaries,
            activeFilterLabel = selectedTeacherFilter.label,
            onDismiss = { showExportPdfDialog = false }
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
        val contextSummary = remember(schoolOverview) {
            "Rata-rata ketepatan waktu guru: ${schoolOverview.averageOnTimeRatePercent}%, " +
                "Kepatuhan Modul Ajar: ${schoolOverview.modulAjarCompliancePercent}%, " +
                "Total Guru: ${schoolOverview.totalTeachers}, " +
                "Tugas Selesai Tepat Waktu: ${schoolOverview.totalOnTimeTasks}, " +
                "Tugas Perlu Diingatkan/Terlambat: ${schoolOverview.totalLateOrPendingTasks}."
        }
        com.example.ui.components.ColekAiAssistantDialog(
            currentUser = currentUser,
            taskRepository = taskRepository,
            localTaskRepository = localTaskRepository,
            schoolContextSummary = contextSummary,
            onDismiss = { showAiAssistantDialog = false }
        )
    }
}

// -------------------------------------------------------------
// COMPONENT: KARTU PERFORMA GURU (TEACHER PERFORMANCE CARD)
// -------------------------------------------------------------
@Composable
private fun TeacherPerformanceCard(
    summary: TeacherSupervisionSummary,
    teacherTasks: List<Task>,
    onColekClicked: (Task) -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val applicableTeacherTasks = remember(teacherTasks, summary.teacher) {
        teacherTasks.filter { TeacherSupervisionCalculator.isTaskAssignedToTeacher(it, summary.teacher) }
    }

    val rateColor = when {
        summary.onTimeRatePercent >= 80 -> EmeraldDone
        summary.onTimeRatePercent >= 50 -> AmberStreak
        else -> CoralLate
    }

    val disciplineTier = summary.teacher.getDisciplineTier()

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_performance_card_${summary.teacher.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Specialty, and Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(rateColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = disciplineTier.badgeEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = summary.teacher.displayName.ifBlank { "Bapak/Ibu Guru" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Guru ${summary.teacher.subjectSpecialty.ifBlank { "Mata Pelajaran" }} • ${disciplineTier.name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Streak pill
                Box(
                    modifier = Modifier
                        .background(OrangeFlame.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔥 ${summary.teacher.currentDisciplineStreak} streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeFlame
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Performance Metrics Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Metric 1: Tugas Selesai Tepat Waktu
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldDone,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${summary.onTimeCompletedTasks} Tugas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDone
                        )
                    }
                    Text(text = "Tepat Waktu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Metric 2: Total Tugas Ditugaskan
                Column {
                    Text(
                        text = "${summary.completedTasks} / ${summary.totalAssignedTasks}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                    Text(text = "Total Selesai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Metric 3: Tingkat Ketepatan Waktu
                Column {
                    Text(
                        text = "${summary.onTimeRatePercent}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = rateColor
                    )
                    Text(text = "Ketepatan Waktu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Metric 4: Status Modul Ajar
                Column {
                    val (modulText, modulColor) = when (summary.modulAjarStatus) {
                        ModulAjarProgressStatus.COMPLETED_ON_TIME -> "Selesai ✨" to EmeraldDone
                        ModulAjarProgressStatus.COMPLETED_LATE -> "Terlambat ⚠️" to AmberStreak
                        ModulAjarProgressStatus.PENDING -> "Dikerjakan ⏳" to IndigoPrimary
                        ModulAjarProgressStatus.OVERDUE -> "Terlambat ❌" to CoralLate
                        ModulAjarProgressStatus.NO_TASK -> "Belum Ada" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = modulText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = modulColor
                    )
                    Text(text = "Modul Ajar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeliness Progress Bar
            LinearProgressIndicator(
                progress = { (summary.onTimeRatePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = rateColor,
                trackColor = rateColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Detail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { expandedDetails = !expandedDetails }
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (expandedDetails) "Tutup Rincian" else "Lihat Rincian Tugas (${summary.totalAssignedTasks})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Colek Guru Button
                val pendingTask = applicableTeacherTasks.firstOrNull { task ->
                    summary.recentSubmissions.none { it.taskId == task.id && it.status == SubmissionStatus.COMPLETED.name }
                } ?: applicableTeacherTasks.firstOrNull()

                if (pendingTask != null && (summary.pendingTasks > 0 || summary.lateTasks > 0)) {
                    FilledTonalButton(
                        onClick = { onColekClicked(pendingTask) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = OrangeFlame.copy(alpha = 0.15f),
                            contentColor = OrangeFlame
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("colek_button_${summary.teacher.id}")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Colek Guru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (summary.totalAssignedTasks > 0 && summary.completedTasks == summary.totalAssignedTasks) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldDone, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Semua Tuntas ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDone)
                    }
                }
            }

            // Expanded Breakdown: Shows individual tasks for this teacher
            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Status Per Tugas:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (applicableTeacherTasks.isEmpty()) {
                        Text("Belum ada tugas yang diberikan.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        applicableTeacherTasks.forEach { task ->
                            val sub = summary.recentSubmissions.firstOrNull { it.taskId == task.id }
                            val isCompleted = sub != null && sub.status == SubmissionStatus.COMPLETED.name
                            val isLate = sub?.status == SubmissionStatus.LATE.name || (sub != null && !sub.isOntime)
                            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
                            val isOverdue = !isCompleted && dueMillis in 1 until System.currentTimeMillis()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isCompleted && !isLate -> Icons.Default.CheckCircle
                                            isCompleted && isLate -> Icons.Default.Warning
                                            isOverdue -> Icons.Default.Warning
                                            else -> Icons.Default.HourglassBottom
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted && !isLate -> EmeraldDone
                                            isCompleted && isLate -> AmberStreak
                                            isOverdue -> CoralLate
                                            else -> IndigoPrimary
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = task.subjectOrCategory.ifBlank { "Administrasi" },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = when {
                                        isCompleted && !isLate -> "Tepat Waktu ✨"
                                        isCompleted && isLate -> "Selesai Terlambat"
                                        isOverdue -> "Terlambat ⚠️"
                                        else -> "Belum Selesai"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isCompleted && !isLate -> EmeraldDone
                                        isCompleted && isLate -> AmberStreak
                                        isOverdue -> CoralLate
                                        else -> OrangeFlame
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: KARTU MODUL AJAR PER GURU
// -------------------------------------------------------------
@Composable
private fun ModulAjarTeacherCard(
    summary: TeacherSupervisionSummary,
    modulTasks: List<Task>,
    onColekTeacher: (Task) -> Unit
) {
    val (statusLabel, statusColor, statusIcon) = when (summary.modulAjarStatus) {
        ModulAjarProgressStatus.COMPLETED_ON_TIME -> Triple("Selesai Tepat Waktu", EmeraldDone, Icons.Default.CheckCircle)
        ModulAjarProgressStatus.COMPLETED_LATE -> Triple("Selesai Terlambat", AmberStreak, Icons.Default.Warning)
        ModulAjarProgressStatus.PENDING -> Triple("Sedang Dikerjakan", IndigoPrimary, Icons.Default.HourglassBottom)
        ModulAjarProgressStatus.OVERDUE -> Triple("Terlambat Belum Mengumpulkan", CoralLate, Icons.Default.Warning)
        ModulAjarProgressStatus.NO_TASK -> Triple("Belum Ada Tugas Modul", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.MenuBook)
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("modul_teacher_card_${summary.teacher.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = summary.teacher.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Guru ${summary.teacher.subjectSpecialty.ifBlank { "Mata Pelajaran" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modul Ajar Specific Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progres Modul: ${summary.modulAjarCompleted} dari ${summary.modulAjarTotal} Modul (${summary.modulAjarOnTime} tepat waktu)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val applicableModulTasks = remember(modulTasks, summary.teacher) {
                    modulTasks.filter { TeacherSupervisionCalculator.isTaskAssignedToTeacher(it, summary.teacher) }
                }

                if (summary.modulAjarStatus != ModulAjarProgressStatus.COMPLETED_ON_TIME && applicableModulTasks.isNotEmpty()) {
                    val taskToNudge = applicableModulTasks.firstOrNull { task ->
                        summary.recentSubmissions.none { it.taskId == task.id && it.status == SubmissionStatus.COMPLETED.name }
                    } ?: applicableModulTasks.first()

                    FilledTonalButton(
                        onClick = { onColekTeacher(taskToNudge) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = OrangeFlame.copy(alpha = 0.15f),
                            contentColor = OrangeFlame
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("colek_modul_${summary.teacher.id}")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Colek Modul", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (summary.modulAjarStatus == ModulAjarProgressStatus.COMPLETED_ON_TIME) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldDone, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terverifikasi Kepala Sekolah", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldDone)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: KARTU TUGAS ADMINISTRASI (ADMIN TASK ITEM)
// -------------------------------------------------------------
@Composable
private fun AdminTaskItemCard(
    task: Task,
    teachers: List<User>,
    submissions: List<com.example.model.TaskSubmission>,
    onColekTeacher: (User) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val targetedTeachers = remember(task.targetFilter, teachers) {
        val matched = teachers.filter { TeacherSupervisionCalculator.isTaskAssignedToTeacher(task, it) }
        if (matched.isEmpty() && (task.targetFilter.isBlank() || task.targetFilter.contains("SEMUA", ignoreCase = true))) {
            teachers
        } else {
            matched
        }
    }

    val taskSubmissions = remember(task.id, submissions, targetedTeachers) {
        val targetIds = targetedTeachers.map { it.id }.toSet()
        submissions.filter { it.taskId == task.id && (targetIds.isEmpty() || it.userId in targetIds) }
    }
    val completedCount = taskSubmissions.count { it.status == SubmissionStatus.COMPLETED.name }
    val totalTeachers = targetedTeachers.size.coerceAtLeast(1)
    val progressPercent = ((completedCount.toDouble() / totalTeachers.toDouble()) * 100).toInt()
    val targetLabel = task.targetFilter.ifBlank { "SEMUA GURU" }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(PrincipalTeal.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.subjectOrCategory.ifBlank { "Administrasi" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrincipalTeal
                    )
                }
                Text(
                    text = "$completedCount / $totalTeachers Guru Selesai",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progressPercent >= 80) EmeraldDone else OrangeFlame
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Target Sekolah & Target Guru Row
            if (task.targetSchoolName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = PrincipalTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sekolah: ${task.targetSchoolName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrincipalTeal
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Target Guru: $targetLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IndigoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progressPercent >= 80) EmeraldDone else OrangeFlame,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Teacher Checklist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Sembunyikan Status Pengumpulan" else "Lihat Status Pengumpulan Guru (${targetedTeachers.size} Guru)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (targetedTeachers.isEmpty()) {
                        Text(
                            text = "Ditugaskan ke: $targetLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        targetedTeachers.forEach { teacher ->
                            val teacherSubmission = taskSubmissions.firstOrNull { it.userId == teacher.id }
                            val isDone = teacherSubmission?.status == SubmissionStatus.COMPLETED.name

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = if (isDone) EmeraldDone else OrangeFlame,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = teacher.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (teacher.subjectSpecialty.isNotBlank()) {
                                            Text(
                                                text = "Guru ${teacher.subjectSpecialty}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (!isDone) {
                                    FilledTonalButton(
                                        onClick = { onColekTeacher(teacher) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = OrangeFlame.copy(alpha = 0.15f),
                                            contentColor = OrangeFlame
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Colek", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "Selesai ✨",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDone
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: TARGET GURU & KUSTOM BATAS HARI SELECTOR
// -------------------------------------------------------------
@Composable
private fun TeacherTargetAndDeadlineSelector(
    managedSchools: List<String>,
    selectedTargetSchools: MutableList<String>,
    onAddNewSchool: (String) -> Unit,
    teachers: List<User>,
    targetMode: String,
    onTargetModeChange: (String) -> Unit,
    selectedTeacherNames: MutableList<String>,
    manualTeacherTarget: String,
    onManualTeacherTargetChange: (String) -> Unit,
    daysDeadline: Int,
    onDaysDeadlineChange: (Int) -> Unit,
    customDaysInput: String,
    onCustomDaysInputChange: (String) -> Unit,
    isCustomDaysMode: Boolean,
    onCustomDaysModeChange: (Boolean) -> Unit,
    tagPrefix: String
) {
    val presetDays = listOf(1, 3, 5, 7, 14, 30)
    var inlineSchoolName by remember { mutableStateOf("") }

    Text("Pilih Sekolah Tujuan (Bisa Pilih Sekolah Berbeda):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            val allSchoolsSelected = selectedTargetSchools.size == managedSchools.size && managedSchools.size > 1
            FilterChip(
                selected = allSchoolsSelected,
                onClick = {
                    if (allSchoolsSelected) {
                        val first = managedSchools.firstOrNull()
                        selectedTargetSchools.clear()
                        if (first != null) selectedTargetSchools.add(first)
                    } else {
                        selectedTargetSchools.clear()
                        selectedTargetSchools.addAll(managedSchools)
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
                    if (isSelected && selectedTargetSchools.size > 1) {
                        selectedTargetSchools.remove(school)
                    } else if (!isSelected) {
                        selectedTargetSchools.add(school)
                    }
                },
                label = { Text(if (isSelected) "✓ 🏫 $school" else "🏫 $school", fontSize = 11.sp) }
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
            value = inlineSchoolName,
            onValueChange = { inlineSchoolName = it },
            label = { Text("Buat Nama Sekolah Baru (Opsional)", fontSize = 11.sp) },
            placeholder = { Text("Contoh: SMA Negeri 2 Harapan", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("${tagPrefix}_inline_new_school_input")
        )
        FilledTonalButton(
            onClick = {
                val clean = inlineSchoolName.trim()
                if (clean.isNotBlank()) {
                    onAddNewSchool(clean)
                    inlineSchoolName = ""
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("${tagPrefix}_inline_add_school_btn")
        ) {
            Text("+ Sekolah", fontSize = 11.sp)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("Target Penugasan Guru:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = targetMode == "SEMUA GURU",
            onClick = { onTargetModeChange("SEMUA GURU") },
            label = { Text("Semua Guru", fontSize = 12.sp) },
            modifier = Modifier.testTag("${tagPrefix}_target_all_teachers")
        )
        FilterChip(
            selected = targetMode == "PILIH GURU",
            onClick = { onTargetModeChange("PILIH GURU") },
            label = {
                Text(
                    text = if (selectedTeacherNames.isEmpty()) "Pilih Guru Tertentu" else "Guru Terpilih (${selectedTeacherNames.size})",
                    fontSize = 12.sp
                )
            },
            modifier = Modifier.testTag("${tagPrefix}_target_specific_teachers")
        )
    }

    AnimatedVisibility(visible = targetMode == "PILIH GURU") {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Guru Tujuan (${selectedTeacherNames.size} dipilih):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (teachers.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Pilih Semua",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrincipalTeal,
                                modifier = Modifier.clickable {
                                    selectedTeacherNames.clear()
                                    selectedTeacherNames.addAll(teachers.map { it.displayName })
                                }
                            )
                            if (selectedTeacherNames.isNotEmpty()) {
                                Text(
                                    text = "Reset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CoralLate,
                                    modifier = Modifier.clickable {
                                        selectedTeacherNames.clear()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (teachers.isEmpty()) {
                    Text(
                        text = "Belum ada guru terdaftar di daftar anggota. Ketik nama guru atau mata pelajaran di bawah:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    teachers.forEach { teacher ->
                        val isChecked = teacher.displayName in selectedTeacherNames
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isChecked) {
                                        selectedTeacherNames.remove(teacher.displayName)
                                    } else {
                                        selectedTeacherNames.add(teacher.displayName)
                                    }
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                                .testTag("${tagPrefix}_teacher_option_${teacher.id}")
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (teacher.displayName !in selectedTeacherNames) {
                                            selectedTeacherNames.add(teacher.displayName)
                                        }
                                    } else {
                                        selectedTeacherNames.remove(teacher.displayName)
                                    }
                                }
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(
                                    text = teacher.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (teacher.subjectSpecialty.isNotBlank()) {
                                    Text(
                                        text = "Guru ${teacher.subjectSpecialty}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualTeacherTarget,
                    onValueChange = onManualTeacherTargetChange,
                    label = { Text("Ketik Nama Guru / Mapel Tambahan (Opsional)", fontSize = 11.sp) },
                    placeholder = { Text("Misal: Budi Santoso, Matematika", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tagPrefix}_manual_teacher_input"),
                    singleLine = true
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Batas Waktu Pengerjaan: $daysDeadline Hari lagi",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(presetDays) { days ->
            FilterChip(
                selected = !isCustomDaysMode && daysDeadline == days,
                onClick = {
                    onCustomDaysModeChange(false)
                    onDaysDeadlineChange(days)
                },
                label = { Text("$days Hari", fontSize = 11.sp) },
                modifier = Modifier.testTag("${tagPrefix}_deadline_chip_$days")
            )
        }
        item {
            FilterChip(
                selected = isCustomDaysMode || daysDeadline !in presetDays,
                onClick = { onCustomDaysModeChange(true) },
                label = { Text("Kustom Hari", fontSize = 11.sp) },
                modifier = Modifier.testTag("${tagPrefix}_deadline_chip_custom")
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = customDaysInput,
        onValueChange = { raw ->
            onCustomDaysModeChange(true)
            onCustomDaysInputChange(raw)
        },
        label = { Text("Pilihan Kustom Batas Hari (1 - 365 Hari)") },
        placeholder = { Text("Masukkan jumlah hari (misal: 2, 10, 21, 45)") },
        suffix = { Text("Hari", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_custom_deadline_input")
    )
}

// -------------------------------------------------------------
// COMPONENT: MODUL STATUS PILL
// -------------------------------------------------------------
@Composable
private fun ModulStatusPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count Guru",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
