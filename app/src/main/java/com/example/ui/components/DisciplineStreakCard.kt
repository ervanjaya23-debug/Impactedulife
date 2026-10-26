package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColekTemplates
import com.example.model.User
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.OrangeFlame

@Composable
fun DisciplineStreakCard(
    user: User,
    modifier: Modifier = Modifier,
    onViewTiersClick: (() -> Unit)? = null
) {
    val streak = user.currentDisciplineStreak
    val tier = user.getDisciplineTier()
    val progress = tier.calculateProgress(streak)
    val remaining = tier.tasksRemaining(streak)

    var showTiersDialog by remember { mutableStateOf(false) }

    // Pulsing animation for the flame badge
    val infiniteTransition = rememberInfiniteTransition(label = "flame_pulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            OrangeFlame,
            AmberStreak,
            Color(0xFFFF8F00)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { showTiersDialog = true }
            .testTag("discipline_streak_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row: Flame Icon + Streak Count + Tier Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pulsing Flame Container
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(flameScale)
                            .background(Color.White.copy(alpha = 0.25f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Api Kedisiplinan",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$streak TUGAS",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                            // Tier Pill Badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${tier.badgeEmoji} ${tier.name}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Streak Kedisiplinan Tepat Waktu",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar towards Next Milestone Tier
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (remaining > 0L) {
                                "Target: ${tier.nextTarget} tugas ($remaining tugas lagi)"
                            } else {
                                "Target Peringkat Maksimal Tercapai! 🏆"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Customized smooth Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Milestone Indicators (3, 7, 15, 30)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MilestoneStep(target = 3L, current = streak, label = "3 🌱")
                        MilestoneStep(target = 7L, current = streak, label = "7 ⚡")
                        MilestoneStep(target = 15L, current = streak, label = "15 🔥")
                        MilestoneStep(target = 30L, current = streak, label = "30 👑")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metric Chips Row: Best Streak, Total On-Time, Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DisciplineStatItem(
                        icon = Icons.Default.EmojiEvents,
                        label = "Rekor Terbaik",
                        value = "${user.bestStreakCount} Tugas",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DisciplineStatItem(
                        icon = Icons.Default.Timer,
                        label = "Tepat Waktu",
                        value = "${user.totalOnTimeCompleted} Selesai",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DisciplineStatItem(
                        icon = Icons.Default.Stars,
                        label = "Tingkat Disiplin",
                        value = "${user.onTimeRatePercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Encouragement Text
                Text(
                    text = ColekTemplates.getStreakMotivation(streak),
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // Modal Panduan Tingkat Kedisiplinan
    if (showTiersDialog) {
        DisciplineTiersInfoDialog(
            currentStreak = streak,
            onDismiss = { showTiersDialog = false }
        )
    }
}

@Composable
private fun MilestoneStep(
    target: Long,
    current: Long,
    label: String
) {
    val reached = current >= target
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    if (reached) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (reached) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OrangeFlame,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (reached) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DisciplineStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DisciplineTiersInfoDialog(
    currentStreak: Long,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥 Tingkat Streak Kedisiplinan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Selesaikan tugas sekolah tepat sebelum deadline untuk meningkatkan streak dan meraih penghargaan kedisiplinan!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                TierRowItem(
                    emoji = "🌱",
                    title = "Pemula Disiplin",
                    requirement = "0 - 2 Tugas On-Time",
                    isCurrent = currentStreak in 0L..2L
                )
                TierRowItem(
                    emoji = "⚡",
                    title = "Pejuang Waktu",
                    requirement = "3 - 6 Tugas On-Time",
                    isCurrent = currentStreak in 3L..6L
                )
                TierRowItem(
                    emoji = "🔥",
                    title = "Bintang Teladan",
                    requirement = "7 - 14 Tugas On-Time",
                    isCurrent = currentStreak in 7L..14L
                )
                TierRowItem(
                    emoji = "👑",
                    title = "Master Kedisiplinan",
                    requirement = "15 - 29 Tugas On-Time",
                    isCurrent = currentStreak in 15L..29L
                )
                TierRowItem(
                    emoji = "💎",
                    title = "Legenda Kedisiplinan",
                    requirement = "30+ Tugas On-Time",
                    isCurrent = currentStreak >= 30L
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "💡 Tips: Pasang Pengingat Colek di setiap tugas agar tidak terlewat deadline!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mengerti & Siap Disiplin!")
            }
        }
    )
}

@Composable
private fun TierRowItem(
    emoji: String,
    title: String,
    requirement: String,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (isCurrent) OrangeFlame.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) OrangeFlame else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = requirement,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .background(OrangeFlame, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Posisi Kamu",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
