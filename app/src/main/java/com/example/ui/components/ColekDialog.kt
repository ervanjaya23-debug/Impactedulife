package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ColekSantunTone
import com.example.model.ColekTemplates
import com.example.model.DeadlineUrgency
import com.example.model.Task
import com.example.model.User
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CoralLate
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.OrangeFlame

@Composable
fun ColekDialog(
    task: Task,
    candidates: List<User>,
    currentUserRole: String,
    onSendNudge: (targetUser: User, templateText: String) -> Unit,
    onDismiss: () -> Unit,
    getRemainingQuota: suspend (targetUserId: String) -> Int
) {
    var selectedUser by remember { mutableStateOf(candidates.firstOrNull()) }
    var remainingQuota by remember { mutableIntStateOf(3) }
    var isLoadingQuota by remember { mutableStateOf(false) }

    val dueMillis = remember(task.dueDate) { task.dueDate?.toDate()?.time ?: 0L }
    val now = remember { System.currentTimeMillis() }
    val remainingTimeStr = remember(dueMillis, now) { ColekTemplates.calculateRemainingTime(dueMillis, now) }
    val isApproaching = remember(dueMillis, now) { ColekTemplates.isApproachingDeadline(dueMillis, now) }
    val urgency = remember(dueMillis, now) { ColekTemplates.getDeadlineUrgency(dueMillis, now) }

    // 0: Otomatis Santun, 1: Formal & Hormat, 2: Santun & Suportif, 3: Ramah Santun, 4: Lugas & Cepat
    var selectedToneIndex by remember { mutableIntStateOf(0) }
    var selectedTemplateIndex by remember { mutableIntStateOf(0) }
    var isCustomizingText by remember { mutableStateOf(false) }
    var customMessageText by remember { mutableStateOf("") }

    val targetName = selectedUser?.displayName ?: "Rekan"
    val targetRole = selectedUser?.role ?: "STUDENT"

    // Auto-generated polite message
    val autoMessage = remember(currentUserRole, targetRole, targetName, task.title, dueMillis) {
        ColekTemplates.buildAutoPoliteMessage(
            senderRole = currentUserRole,
            targetRole = targetRole,
            targetName = targetName,
            taskTitle = task.title,
            dueMillis = dueMillis,
            now = now
        )
    }

    // Templates for the selected tone category
    val currentTemplates = remember(selectedToneIndex, targetName, task.title, dueMillis) {
        when (selectedToneIndex) {
            1 -> ColekTemplates.getTemplatesForTone(ColekSantunTone.FORMAL_RESPECTFUL, targetName, task.title, dueMillis, now)
            2 -> ColekTemplates.getTemplatesForTone(ColekSantunTone.POLITE_SUPPORTIVE, targetName, task.title, dueMillis, now)
            3 -> ColekTemplates.getTemplatesForTone(ColekSantunTone.PEER_COURTEOUS, targetName, task.title, dueMillis, now)
            4 -> ColekTemplates.getTemplatesForTone(ColekSantunTone.QUICK_PROFESSIONAL, targetName, task.title, dueMillis, now)
            else -> listOf(autoMessage)
        }
    }

    // Active message text that will be sent
    val finalMessage = if (isCustomizingText) {
        customMessageText
    } else if (selectedToneIndex == 0) {
        autoMessage
    } else {
        currentTemplates.getOrElse(selectedTemplateIndex) { autoMessage }
    }

    LaunchedEffect(selectedUser) {
        val user = selectedUser
        if (user != null) {
            isLoadingQuota = true
            remainingQuota = getRemainingQuota(user.id)
            isLoadingQuota = false
        }
    }

    // Initialize custom message if edited
    LaunchedEffect(selectedToneIndex, selectedTemplateIndex, autoMessage) {
        if (!isCustomizingText) {
            customMessageText = if (selectedToneIndex == 0) {
                autoMessage
            } else {
                currentTemplates.getOrElse(selectedTemplateIndex) { autoMessage }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("colek_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Title & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(OrangeFlame.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = OrangeFlame,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Colek Santun",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pengingat sopan & profesional",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_colek_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Deadline Urgency Banner
                val bannerBg = when (urgency) {
                    DeadlineUrgency.CRITICAL -> CoralLate.copy(alpha = 0.15f)
                    DeadlineUrgency.HIGH -> AmberStreak.copy(alpha = 0.18f)
                    DeadlineUrgency.OVERDUE -> CoralLate.copy(alpha = 0.15f)
                    else -> BlueAccent.copy(alpha = 0.12f)
                }
                val bannerFg = when (urgency) {
                    DeadlineUrgency.CRITICAL, DeadlineUrgency.OVERDUE -> CoralLate
                    DeadlineUrgency.HIGH -> Color(0xFFD97706)
                    else -> BlueAccent
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deadline_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isApproaching) Icons.Default.HourglassTop else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = bannerFg,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = if (isApproaching) "⏳ Mendekati tenggat: Sisa $remainingTimeStr" else "⏰ Tenggat: $remainingTimeStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = bannerFg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target User Selection
                Text(
                    text = if (currentUserRole == "PRINCIPAL") "Tujuan Dewan Guru:" else "Tujuan Rekan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (candidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada rekan yang dapat dicolek pada tugas ini",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (candidates.size == 1) {
                    val user = candidates.first()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = user.displayName.ifBlank { "Pengguna" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(72.dp)) {
                        items(candidates) { user ->
                            val isSelected = user.id == selectedUser?.id
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedUser = user }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("candidate_${user.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.displayName.ifBlank { "Pengguna" },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tone Filter Chips
                Text(
                    text = "Pilih Nada Bahasa Santun:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tones = listOf(
                        Triple(0, "🌟 Otomatis", "auto"),
                        Triple(1, "🎩 Formal & Hormat", "formal"),
                        Triple(2, "🤝 Santun Suportif", "supportive"),
                        Triple(3, "✨ Ramah Teman", "peer"),
                        Triple(4, "⏱️ Lugas & Cepat", "direct")
                    )

                    tones.forEach { (index, label, tag) ->
                        FilterChip(
                            selected = selectedToneIndex == index,
                            onClick = {
                                selectedToneIndex = index
                                selectedTemplateIndex = 0
                                isCustomizingText = false
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeFlame.copy(alpha = 0.18f),
                                selectedLabelColor = OrangeFlame
                            ),
                            modifier = Modifier.testTag("tone_chip_$tag")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template / Custom Editor Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isCustomizingText) "Edit Pesan Santun:" else "Pilihan Pesan Sopan & Profesional:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                isCustomizingText = !isCustomizingText
                                if (isCustomizingText && customMessageText.isBlank()) {
                                    customMessageText = finalMessage
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Pesan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isCustomizingText) "Pakai Template" else "Sesuaikan Kata",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isCustomizingText) {
                    OutlinedTextField(
                        value = customMessageText,
                        onValueChange = { customMessageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("custom_message_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                } else if (selectedToneIndex == 0) {
                    // Auto-recommended banner
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_recommend_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = OrangeFlame,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rekomendasi Otomatis Colek Santun",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeFlame
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"$autoMessage\"",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Templates list for selected tone
                    LazyColumn(modifier = Modifier.height(130.dp)) {
                        items(currentTemplates.indices.toList()) { index ->
                            val isSelected = index == selectedTemplateIndex
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .clickable { selectedTemplateIndex = index }
                                    .padding(8.dp)
                                    .testTag("template_item_$index")
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplateIndex = index },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentTemplates[index],
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quota Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Batas etika pengingat harian:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isLoadingQuota) "Memeriksa..." else "Sisa kuota: $remainingQuota/3",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingQuota > 0) EmeraldDone else CoralLate,
                        modifier = Modifier.testTag("quota_indicator")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Cancel & Send
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("cancel_colek_button")
                    ) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val user = selectedUser
                            if (user != null && remainingQuota > 0 && finalMessage.isNotBlank()) {
                                onSendNudge(user, finalMessage)
                            }
                        },
                        enabled = selectedUser != null && remainingQuota > 0 && finalMessage.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeFlame,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("send_colek_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim Colek Santun")
                    }
                }
            }
        }
    }
}
