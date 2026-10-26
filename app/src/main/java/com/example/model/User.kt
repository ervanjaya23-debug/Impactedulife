package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    STUDENT,
    TEACHER,
    PRINCIPAL,
    SCHOOL_ADMIN,
    SUPER_ADMIN
}

enum class UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val role: String = UserRole.STUDENT.name,
    val status: String = UserStatus.PENDING.name,
    val className: String = "",
    val subjectSpecialty: String = "",
    val managedSchools: List<String> = emptyList(),
    val managedClasses: List<String> = emptyList(),
    val streakCount: Long = 0L,
    val onTimeStreakCount: Long = 0L,
    val bestStreakCount: Long = 0L,
    val totalOnTimeCompleted: Long = 0L,
    val totalTasksCompleted: Long = 0L,
    val lastOnTimeTaskDate: String = "",
    val lastActiveDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val isApproved: Boolean get() = status == UserStatus.ACTIVE.name
    val isStudent: Boolean get() = role == UserRole.STUDENT.name
    val isTeacher: Boolean get() = role == UserRole.TEACHER.name
    val isPrincipal: Boolean get() = role == UserRole.PRINCIPAL.name
    val isAdmin: Boolean get() = role == UserRole.SCHOOL_ADMIN.name || role == UserRole.SUPER_ADMIN.name

    val effectiveSchools: List<String>
        get() {
            val combined = buildList {
                if (schoolName.isNotBlank()) add(schoolName.trim())
                managedSchools.forEach { s ->
                    if (s.isNotBlank()) add(s.trim())
                }
            }.distinct()
            return if (combined.isEmpty()) listOf("SMA Negeri 1 Teladan") else combined
        }

    val effectiveClasses: List<String>
        get() {
            val combined = buildList {
                if (className.isNotBlank()) {
                    className.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(it) }
                }
                managedClasses.forEach { c ->
                    if (c.isNotBlank()) add(c.trim())
                }
            }.distinct()
            return if (combined.isEmpty()) listOf("10-IPA-1", "10-IPA-2", "11-IPA-1") else combined
        }

    val currentDisciplineStreak: Long get() = if (onTimeStreakCount > 0L) onTimeStreakCount else streakCount

    val onTimeRatePercent: Int get() = if (totalTasksCompleted > 0L) {
        ((totalOnTimeCompleted.toDouble() / totalTasksCompleted.toDouble()) * 100).toInt()
    } else {
        100
    }

    fun getDisciplineTier(): DisciplineTier {
        val streak = currentDisciplineStreak
        return when {
            streak < 3L -> DisciplineTier(
                name = "Pemula Disiplin",
                tagline = "Langkah awal pembiasaan tepat waktu",
                minStreak = 0L,
                nextTarget = 3L,
                badgeEmoji = "🌱"
            )
            streak < 7L -> DisciplineTier(
                name = "Pejuang Waktu",
                tagline = "Fokus dan konsisten menyelesaikan tugas",
                minStreak = 3L,
                nextTarget = 7L,
                badgeEmoji = "⚡"
            )
            streak < 15L -> DisciplineTier(
                name = "Bintang Teladan",
                tagline = "Inspirasi ketepatan waktu bagi teman sekelas",
                minStreak = 7L,
                nextTarget = 15L,
                badgeEmoji = "🔥"
            )
            streak < 30L -> DisciplineTier(
                name = "Master Kedisiplinan",
                tagline = "Juara ketepatan waktu sekolah tanpa kompromi",
                minStreak = 15L,
                nextTarget = 30L,
                badgeEmoji = "👑"
            )
            else -> DisciplineTier(
                name = "Legenda Kedisiplinan",
                tagline = "Puncak kedisiplinan dan teladan abadi sekolah",
                minStreak = 30L,
                nextTarget = 50L,
                badgeEmoji = "💎"
            )
        }
    }
}

data class DisciplineTier(
    val name: String,
    val tagline: String,
    val minStreak: Long,
    val nextTarget: Long,
    val badgeEmoji: String
) {
    fun calculateProgress(streak: Long): Float {
        if (streak >= nextTarget) return 1f
        if (nextTarget <= minStreak) return 1f
        val currentProgress = (streak - minStreak).coerceAtLeast(0L)
        val needed = (nextTarget - minStreak)
        return (currentProgress.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    }

    fun tasksRemaining(streak: Long): Long {
        return (nextTarget - streak).coerceAtLeast(0L)
    }
}
