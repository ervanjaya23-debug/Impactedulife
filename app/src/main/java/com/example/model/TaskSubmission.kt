package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubmissionStatus {
    IN_PROGRESS,
    COMPLETED,
    LATE
}

@IgnoreExtraProperties
data class TaskSubmission(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val userName: String = "",
    val schoolId: String = "",
    val userRole: String = "",
    val status: String = SubmissionStatus.IN_PROGRESS.name,
    val completedAt: Timestamp? = null,
    val isOntime: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
