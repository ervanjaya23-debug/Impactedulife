package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DisciplineResult
import com.example.model.ColekTemplates
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.EmeraldDone
import com.example.ui.theme.OrangeFlame

@Composable
fun DisciplineCelebrationDialog(
    result: DisciplineResult,
    taskTitle: String,
    onDismiss: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(OrangeFlame, AmberStreak)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("discipline_celebration_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Flame / Badge Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            brush = if (result.isOntime) gradient else Brush.linearGradient(listOf(Color(0xFF78909C), Color(0xFF546E7A))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (result.isOntime) Icons.Default.LocalFireDepartment else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (result.isOntime) "SELESAI TEPAT WAKTU! 🔥" else "Tugas Selesai Tercatat!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (result.isOntime) OrangeFlame else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"$taskTitle\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (result.isOntime) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangeFlame.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STREAK KEDISIPLINAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeFlame,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${result.currentStreak} TUGAS BERUNTUN",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (result.isNewTierReached) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(OrangeFlame, shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "🏆 NAIK PERINGKAT: ${result.newTierName}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = ColekTemplates.getOnTimeSuccessMessage(
                            currentStreak = result.currentStreak,
                            isNewTier = result.isNewTierReached,
                            tierName = result.newTierName
                        ),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Tugas berhasil diselesaikan, namun telah melewati batas waktu pengumpulan. Streak kedisiplinan direset, ayo bangun kembali di tugas berikutnya! 💪",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dismiss_celebration_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (result.isOntime) OrangeFlame else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (result.isOntime) "Lanjutkan Streak! 🚀" else "Tutup & Terus Semangat",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
