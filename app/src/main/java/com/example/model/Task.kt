package com.example.model

import com.example.data.local.TaskEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class TaskScope {
    STUDENT_TASK,    // Dibuat oleh guru untuk siswa
    TEACHER_TASK,    // Dibuat oleh kepala sekolah untuk guru
    PERSONAL_TASK    // Dibuat oleh siswa untuk diri sendiri
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val createdBy: String = "",
    val creatorRole: String = "",
    val creatorName: String = "",
    val scope: String = TaskScope.STUDENT_TASK.name,
    val title: String = "",
    val description: String = "",
    val subjectOrCategory: String = "",
    val targetFilter: String = "", // e.g. "ALL_TEACHERS", "10-IPA-1, 10-IPA-2", "SELF"
    val targetSchoolName: String = "", // e.g. "SMA Negeri 1 Teladan", "SEMUA SEKOLAH"
    val priority: String = TaskPriority.MEDIUM.name,
    val dueDate: Timestamp? = null,
    val reminderOffsets: List<Long> = listOf(1440L, 180L, 30L, 0L), // menit sebelum deadline
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id.ifBlank { java.util.UUID.randomUUID().toString() },
        schoolId = schoolId,
        title = title,
        description = description,
        deadline = dueDate?.toDate()?.time ?: 0L,
        status = "PENDING",
        assignedTeacherOrUser = targetFilter,
        assignedUserRole = if (scope == TaskScope.TEACHER_TASK.name) "TEACHER" else "STUDENT",
        category = subjectOrCategory,
        scope = scope,
        targetClass = targetFilter,
        createdBy = createdBy,
        creatorName = creatorName,
        priority = priority,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun TaskEntity.toTask(): Task {
    return Task(
        id = id,
        schoolId = schoolId,
        createdBy = createdBy,
        creatorRole = if (scope == TaskScope.TEACHER_TASK.name) "PRINCIPAL" else "TEACHER",
        creatorName = creatorName,
        scope = scope,
        title = title,
        description = description,
        subjectOrCategory = category,
        targetFilter = targetClass,
        priority = priority,
        dueDate = if (deadline > 0) Timestamp(Date(deadline)) else null
    )
}
