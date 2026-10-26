package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class NudgeType {
    PEER_STUDENT,
    TEACHER_TO_STUDENT,
    PRINCIPAL_TO_TEACHER
}

@IgnoreExtraProperties
data class Nudge(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromRole: String = "",
    val toUserId: String = "",
    val toUserName: String = "",
    val toRole: String = "",
    val nudgeType: String = NudgeType.PEER_STUDENT.name,
    val taskId: String = "",
    val taskTitle: String = "",
    val templateText: String = "",
    val dateKey: String = "", // e.g. "2026-10-23" for anti-spam quota
    @ServerTimestamp val sentAt: Timestamp? = null
)
