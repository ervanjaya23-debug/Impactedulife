package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColekTemplates
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskScope
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CoralLate
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.OrangeFlame
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskCard(
    task: Task,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onColekClicked: (() -> Unit)? = null,
    onToggleAlarm: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isLate: Boolean = false
) {
    var alarmActive by remember { mutableStateOf(true) }

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH.name -> CoralLate
        TaskPriority.MEDIUM.name -> OrangeFlame
        else -> BlueAccent
    }

    val dueMillis = task.dueDate?.toDate()?.time ?: 0L
    val now = System.currentTimeMillis()
    val isOverdue = dueMillis in 1..now
    val isApproaching = remember(dueMillis, now) { ColekTemplates.isApproachingDeadline(dueMillis, now) }
    val remainingTimeStr = remember(dueMillis, now) { ColekTemplates.calculateRemainingTime(dueMillis, now) }

    val dateFormatted = remember(dueMillis) {
        if (dueMillis == 0L) "Tanpa tenggat"
        else SimpleDateFormat("EEE, dd MMM - HH:mm", Locale("id", "ID")).format(Date(dueMillis))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Subject chip, priority pill, status pill, & Alarm toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Subject Chip
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.subjectOrCategory.ifBlank { "Tugas Umum" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Priority Pill
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Status Pill
                    val (statusLabel, statusBg, statusFg) = when {
                        isCompleted && isLate -> Triple("TERLAMBAT", CoralLate.copy(alpha = 0.15f), CoralLate)
                        isCompleted -> Triple("SELESAI", EmeraldDone.copy(alpha = 0.15f), EmeraldDone)
                        isOverdue -> Triple("TERLEWAT", CoralLate.copy(alpha = 0.15f), CoralLate)
                        else -> Triple("BELUM SELESAI", BlueAccent.copy(alpha = 0.12f), BlueAccent)
                    }
                    Box(
                        modifier = Modifier
                            .background(statusBg, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }

                    if (!isCompleted && isApproaching) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(OrangeFlame.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⏳ SISA $remainingTimeStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeFlame
                            )
                        }
                    }
                }

                // Alarm Icon toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            alarmActive = !alarmActive
                            onToggleAlarm(alarmActive)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (alarmActive) Icons.Default.Alarm else Icons.Default.AlarmOff,
                            contentDescription = "Toggle Alarm",
                            tint = if (alarmActive) OrangeFlame else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task Title
            Text(
                text = task.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.targetSchoolName.isNotBlank() || (task.targetFilter.isNotBlank() && task.scope != TaskScope.PERSONAL_TASK.name)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.targetSchoolName.isNotBlank()) {
                        Text(
                            text = "🏫 ${task.targetSchoolName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (task.targetFilter.isNotBlank() && task.scope != TaskScope.PERSONAL_TASK.name) {
                        Text(
                            text = "🎯 ${task.targetFilter}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deadline and actions row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Due date
                Column {
                    val statusText = when {
                        isCompleted && isLate -> "⚠️ Selesai Terlambat"
                        isCompleted -> "✓ Selesai Tepat Waktu"
                        isOverdue -> "⚠️ Terlewat: $dateFormatted"
                        isApproaching -> "⏳ Mendekati Deadline: $dateFormatted ($remainingTimeStr)"
                        else -> "⏰ Tenggat: $dateFormatted"
                    }
                    val statusColor = when {
                        isCompleted && isLate -> CoralLate
                        isCompleted -> EmeraldDone
                        isOverdue -> CoralLate
                        isApproaching -> OrangeFlame
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = if (isOverdue || isCompleted || isApproaching) FontWeight.Bold else FontWeight.Medium,
                        color = statusColor
                    )
                    if (task.creatorName.isNotBlank() && task.scope != TaskScope.PERSONAL_TASK.name) {
                        Text(
                            text = "Oleh: ${task.creatorName} (${task.creatorRole})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onColekClicked != null && !isCompleted) {
                        FilledTonalButton(
                            onClick = onColekClicked,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isApproaching) OrangeFlame else OrangeFlame.copy(alpha = 0.15f),
                                contentColor = if (isApproaching) Color.White else OrangeFlame
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("colek_santun_button_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Colek Santun",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isApproaching) "Colek Santun" else "Colek",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Complete toggle
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = "Selesai",
                            tint = if (isCompleted) EmeraldDone else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
