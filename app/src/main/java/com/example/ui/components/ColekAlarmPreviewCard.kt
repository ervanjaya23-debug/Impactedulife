package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.ColekAlarmScheduler
import com.example.model.ColekTemplates
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskScope
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OrangeFlame
import com.example.ui.theme.PrincipalTeal
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Attractive banner card and preview dialog showing the visual Alarm & Colek Santun notification
 * complete with the generated 16:9 illustration and 1-tap local notification trigger.
 */
@Composable
fun TeacherApproachingDeadlineAlertCard(
    approachingTasks: List<Task>,
    teacherName: String,
    schoolName: String,
    alarmScheduler: ColekAlarmScheduler,
    onOpenPreviewDialog: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryTask = approachingTasks.firstOrNull() ?: Task(
        id = "sample_teacher_task",
        title = "Pengumpulan Modul Ajar & RPP",
        subjectOrCategory = "Modul Ajar & RPP",
        targetSchoolName = schoolName.ifBlank { "SMA Negeri 1 Teladan" },
        targetFilter = teacherName.ifBlank { "Bapak/Ibu Guru" },
        scope = TaskScope.TEACHER_TASK.name,
        priority = TaskPriority.HIGH.name,
        dueDate = Timestamp(Date(System.currentTimeMillis() + 6 * 3600 * 1000L))
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("teacher_colek_alarm_banner_card")
    ) {
        Column {
            // Attractive Header Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_colek_alarm_banner_1792976051626),
                    contentDescription = "Ilustrasi Alarm Pengingat Colek Santun",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(OrangeFlame, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (approachingTasks.isNotEmpty()) {
                                    "🔔 ${approachingTasks.size} Tugas Mendekati Deadline"
                                } else {
                                    "🔔 Notifikasi Lokal Colek Aktif"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistem Alarm & Colek Santun Otomatis",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Pengingat lokal H-2 Hari, H-1 Hari, H-3 Jam & H-30 Menit",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                val now = System.currentTimeMillis()
                val dueMillis = primaryTask.dueDate?.toDate()?.time ?: (now + 6 * 3600 * 1000L)
                val politePreview = remember(primaryTask.id, teacherName) {
                    ColekTemplates.generateSmartReminder(
                        recipientName = teacherName.ifBlank { "Bapak/Ibu Guru" },
                        taskTitle = primaryTask.title,
                        subjectOrCategory = primaryTask.subjectOrCategory.ifBlank { "Administrasi" },
                        dueMillis = dueMillis,
                        isForTeacher = true,
                        nowMillis = now
                    )
                }

                Text(
                    text = politePreview,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onOpenPreviewDialog(primaryTask) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_alarm_preview_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lihat Desain Alarm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            alarmScheduler.triggerImmediateColekNotification(
                                task = primaryTask,
                                recipientName = teacherName.ifBlank { "Bapak/Ibu Guru" },
                                customMessage = politePreview
                            )
                            Toast.makeText(
                                context,
                                "Notifikasi lokal Colek Santun bergambar dikirim ke panel notifikasi HP! 🔔",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeFlame),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_local_colek_notif_btn")
                    ) {
                        Text("Uji Notifikasi Lokal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog that previews how the Android BigPictureStyle notification looks with the generated
 * illustration and lets the user trigger a live local notification or schedule reminder offsets.
 */
@Composable
fun ColekAlarmVisualPreviewDialog(
    task: Task,
    recipientName: String,
    alarmScheduler: ColekAlarmScheduler,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val dueMillis = task.dueDate?.toDate()?.time ?: (now + 3 * 3600 * 1000L)
    val remainingStr = remember(dueMillis) { ColekTemplates.calculateRemainingTime(dueMillis, now) }
    val politeMessage = remember(task.id, recipientName) {
        ColekTemplates.generateSmartReminder(
            recipientName = recipientName.ifBlank { "Bapak/Ibu Guru" },
            taskTitle = task.title,
            subjectOrCategory = task.subjectOrCategory.ifBlank { "Administrasi" },
            dueMillis = dueMillis,
            isForTeacher = task.scope == TaskScope.TEACHER_TASK.name,
            nowMillis = now
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = OrangeFlame
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tampilan Notifikasi Alarm Colek", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Pratinjau kartu notifikasi sistem Android (BigPictureStyle) yang muncul otomatis saat tugas mendekati batas waktu:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Android System Notification Card with Illustration
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Notification Header Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(PrincipalTeal, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Colek • Pengingat Santun • Sekarang",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrincipalTeal
                                )
                            }
                            Text(
                                text = "Sisa $remainingStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeFlame
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "🔔 Colek Santun: ${task.title}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (task.targetSchoolName.isNotBlank() || task.targetFilter.isNotBlank()) {
                            Text(
                                text = buildString {
                                    if (task.targetSchoolName.isNotBlank()) append("🏫 ${task.targetSchoolName}  ")
                                    if (task.targetFilter.isNotBlank()) append("🎯 ${task.targetFilter}")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndigoPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = politeMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // BigPicture Illustration
                        Image(
                            painter = painterResource(id = R.drawable.img_colek_alarm_banner_1792976051626),
                            contentDescription = "Gambar Banner Notifikasi Alarm Colek",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Notification Action Buttons Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "TUNDA 10 MNT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrincipalTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "BUKA TUGAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = "SELESAI ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDone,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    alarmScheduler.scheduleTaskAlarms(task)
                    alarmScheduler.triggerImmediateColekNotification(
                        task = task,
                        recipientName = recipientName,
                        customMessage = politeMessage
                    )
                    Toast.makeText(
                        context,
                        "Notifikasi Alarm Colek bergambar telah dikirim & dijadwalkan! 🔔",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeFlame)
            ) {
                Text("Kirim Notifikasi Sekarang")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

/**
 * Dialog for Kepala Sekolah (or Guru) to create and manage multiple School Names (Multi-Sekolah),
 * switch the active school displayed in the header, and assign tasks to different schools.
 */
@Composable
fun ManageSchoolsDialog(
    managedSchools: List<String>,
    activeSchoolName: String,
    onSaveSchools: (newActiveSchool: String, updatedSchools: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newSchoolInput by remember { mutableStateOf("") }
    var schoolList by remember(managedSchools) {
        mutableStateOf(
            if (managedSchools.isEmpty()) listOf(activeSchoolName.ifBlank { "SMA Negeri 1 Teladan" })
            else managedSchools
        )
    }
    var selectedPrimary by remember(activeSchoolName, schoolList) {
        mutableStateOf(activeSchoolName.ifBlank { schoolList.firstOrNull().orEmpty() })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = PrincipalTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola & Buat Nama Sekolah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambahkan beberapa nama sekolah/unit berbeda. Anda dapat memilih sekolah utama untuk header dan memberi tugas ke sekolah yang berbeda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSchoolInput,
                        onValueChange = { newSchoolInput = it },
                        label = { Text("Nama Sekolah Baru") },
                        placeholder = { Text("Contoh: SMA Negeri 2 Harapan") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_school_name_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = newSchoolInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (schoolList.none { it.equals(trimmed, ignoreCase = true) }) {
                                    schoolList = schoolList + trimmed
                                    if (selectedPrimary.isBlank()) selectedPrimary = trimmed
                                }
                                newSchoolInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_school_to_list_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Sekolah", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Daftar Sekolah Terdaftar (${schoolList.size} Sekolah):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                schoolList.forEach { school ->
                    val isPrimary = school.equals(selectedPrimary, ignoreCase = true)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPrimary) PrincipalTeal.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { selectedPrimary = school }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isPrimary) Icons.Default.CheckCircle else Icons.Default.School,
                                    contentDescription = null,
                                    tint = if (isPrimary) PrincipalTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = school,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isPrimary) {
                                        Text(
                                            text = "Aktif di Header Dasbor",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrincipalTeal
                                        )
                                    } else {
                                        Text(
                                            text = "Ketuk untuk jadikan aktif di header",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (schoolList.size > 1) {
                                IconButton(
                                    onClick = {
                                        schoolList = schoolList.filterNot { it == school }
                                        if (selectedPrimary == school) {
                                            selectedPrimary = schoolList.firstOrNull().orEmpty()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus Sekolah",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pendingTrimmed = newSchoolInput.trim()
                    val finalSchools = if (pendingTrimmed.isNotBlank() && schoolList.none { it.equals(pendingTrimmed, ignoreCase = true) }) {
                        schoolList + pendingTrimmed
                    } else {
                        schoolList
                    }
                    if (finalSchools.isEmpty()) {
                        Toast.makeText(context, "Minimal harus ada 1 nama sekolah", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalPrimary = if (selectedPrimary in finalSchools) selectedPrimary else finalSchools.first()
                    onSaveSchools(finalPrimary, finalSchools)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrincipalTeal),
                modifier = Modifier.testTag("save_schools_dialog_btn")
            ) {
                Text("Simpan Daftar Sekolah")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Dialog for Guru to create and manage multiple different Class Names (Multi-Kelas).
 */
@Composable
fun ManageClassesDialog(
    managedClasses: List<String>,
    onSaveClasses: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newClassInput by remember { mutableStateOf("") }
    var classList by remember(managedClasses) {
        mutableStateOf(
            if (managedClasses.isEmpty()) listOf("10-IPA-1", "10-IPA-2", "11-IPA-1")
            else managedClasses
        )
    }

    val presetSuggestions = listOf("10-IPA-1", "10-IPA-2", "10-IPS-1", "11-IPA-1", "11-IPA-2", "11-IPS-1", "12-IPA-1", "12-IPS-1")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Class, contentDescription = null, tint = IndigoPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola & Buat Kelas Berbeda", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Buat beberapa nama kelas yang berbeda (bisa ketik satu per satu atau dipisahkan koma, misal: 10-IPA-3, 11-IPS-2).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newClassInput,
                        onValueChange = { newClassInput = it.uppercase() },
                        label = { Text("Nama Kelas Baru") },
                        placeholder = { Text("Contoh: 11-IPA-3, 12-IPS-1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_class_name_input")
                    )
                    Button(
                        onClick = {
                            val entries = newClassInput.split(",")
                                .map { it.trim().uppercase() }
                                .filter { it.isNotEmpty() }
                            if (entries.isNotEmpty()) {
                                val updated = (classList + entries).distinct()
                                classList = updated
                                newClassInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_class_to_list_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Kelas", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Tambah Cepat Kelas:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetSuggestions) { preset ->
                        val exists = preset in classList
                        FilterChip(
                            selected = exists,
                            onClick = {
                                classList = if (exists && classList.size > 1) {
                                    classList - preset
                                } else {
                                    (classList + preset).distinct()
                                }
                            },
                            label = { Text(if (exists) "✓ $preset" else "+ $preset", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Daftar Kelas yang Diampu (${classList.size} Kelas):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                classList.forEach { cls ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Class,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kelas $cls",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (classList.size > 1) {
                                IconButton(
                                    onClick = { classList = classList.filterNot { it == cls } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Kelas $cls",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pendingEntries = newClassInput.split(",")
                        .map { it.trim().uppercase() }
                        .filter { it.isNotEmpty() }
                    val finalClasses = (classList + pendingEntries).distinct()
                    if (finalClasses.isEmpty()) {
                        Toast.makeText(context, "Minimal harus ada 1 kelas", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSaveClasses(finalClasses)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("save_classes_dialog_btn")
            ) {
                Text("Simpan Semua Kelas (${classList.size})")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
