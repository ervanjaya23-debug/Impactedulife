package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val schoolId: String,
    val title: String,
    val description: String,
    val deadline: Long, // timestamp in milliseconds
    val status: String, // PENDING, IN_PROGRESS, COMPLETED, LATE
    val assignedTeacherOrUser: String, // email, user id, or target group like "ALL_TEACHERS"
    val assignedUserRole: String, // TEACHER, STUDENT, ALL
    val category: String, // Modul Ajar, Nilai Raport, PR Harian, dsb
    val scope: String, // TEACHER_TASK, STUDENT_TASK, PERSONAL_TASK
    val targetClass: String, // e.g. "10-IPA-1", "10-IPA-2", "SEMUA GURU"
    val createdBy: String,
    val creatorName: String,
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis()
)
