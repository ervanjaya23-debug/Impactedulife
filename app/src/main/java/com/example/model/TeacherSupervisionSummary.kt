package com.example.model

enum class ModulAjarProgressStatus(val label: String) {
    COMPLETED_ON_TIME("Selesai Tepat Waktu"),
    COMPLETED_LATE("Selesai Terlambat"),
    PENDING("Sedang Dikerjakan"),
    OVERDUE("Terlambat Belum Dikumpulkan"),
    NO_TASK("Belum Ada Tugas")
}

data class TeacherSupervisionSummary(
    val teacher: User,
    val totalAssignedTasks: Int,
    val completedTasks: Int,
    val onTimeCompletedTasks: Int,
    val lateTasks: Int,
    val pendingTasks: Int,
    val onTimeRatePercent: Int,
    val modulAjarTotal: Int,
    val modulAjarCompleted: Int,
    val modulAjarOnTime: Int,
    val modulAjarStatus: ModulAjarProgressStatus,
    val recentSubmissions: List<TaskSubmission> = emptyList()
)

data class SchoolSupervisionOverview(
    val totalTeachers: Int,
    val activeTeachers: Int,
    val averageOnTimeRatePercent: Int,
    val totalOnTimeTasks: Int,
    val totalLateOrPendingTasks: Int,
    val totalModulAjarAssigned: Int,
    val totalModulAjarCompletedOnTime: Int,
    val modulAjarCompliancePercent: Int
)

object TeacherSupervisionCalculator {

    /**
     * Checks whether a principal-assigned teacher task applies to a given [teacher].
     * Matches if targetFilter is blank, contains "SEMUA", or matches the teacher's
     * displayName, email, id, or subjectSpecialty.
     */
    fun isTaskAssignedToTeacher(task: Task, teacher: User): Boolean {
        val filter = task.targetFilter.trim()
        if (filter.isEmpty() || filter.contains("SEMUA", ignoreCase = true)) {
            return true
        }
        return (teacher.displayName.isNotBlank() && filter.contains(teacher.displayName, ignoreCase = true)) ||
            (teacher.email.isNotBlank() && filter.contains(teacher.email, ignoreCase = true)) ||
            (teacher.id.isNotBlank() && filter.contains(teacher.id, ignoreCase = true)) ||
            (teacher.subjectSpecialty.isNotBlank() && filter.contains(teacher.subjectSpecialty, ignoreCase = true))
    }

    fun calculateTeacherSummary(
        teacher: User,
        teacherTasks: List<Task>,
        submissions: List<TaskSubmission>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): TeacherSupervisionSummary {
        // Filter tasks assigned to teachers or specifically to this teacher
        val applicableTasks = teacherTasks.filter { task ->
            isTaskAssignedToTeacher(task, teacher)
        }

        val teacherSubmissions = submissions.filter { it.userId == teacher.id }
        val submissionMap = teacherSubmissions.associateBy { it.taskId }

        var onTimeCount = 0
        var lateCount = 0
        var pendingCount = 0

        applicableTasks.forEach { task ->
            val sub = submissionMap[task.id]
            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
            val isOverdue = dueMillis in 1 until currentTimeMillis
            if (sub != null) {
                if (sub.status == SubmissionStatus.COMPLETED.name && sub.isOntime) {
                    onTimeCount++
                } else {
                    lateCount++
                }
            } else {
                if (isOverdue) {
                    lateCount++
                } else {
                    pendingCount++
                }
            }
        }

        val totalCompleted = teacherSubmissions.count {
            it.status == SubmissionStatus.COMPLETED.name || it.status == SubmissionStatus.LATE.name
        }

        val onTimeRate = if (applicableTasks.isNotEmpty()) {
            ((onTimeCount.toDouble() / applicableTasks.size.toDouble()) * 100).toInt()
        } else if (teacher.totalTasksCompleted > 0) {
            teacher.onTimeRatePercent
        } else {
            100
        }

        // Modul Ajar specific calculation
        val modulTasks = applicableTasks.filter {
            it.subjectOrCategory.contains("Modul", ignoreCase = true) ||
            it.title.contains("Modul", ignoreCase = true) ||
            it.subjectOrCategory.contains("RPP", ignoreCase = true)
        }

        var modulOnTime = 0
        var modulCompleted = 0
        var modulOverdue = false
        var modulHasPending = false

        modulTasks.forEach { task ->
            val sub = submissionMap[task.id]
            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
            val isOverdue = dueMillis in 1 until currentTimeMillis
            if (sub != null && (sub.status == SubmissionStatus.COMPLETED.name || sub.status == SubmissionStatus.LATE.name)) {
                modulCompleted++
                if (sub.isOntime && sub.status == SubmissionStatus.COMPLETED.name) {
                    modulOnTime++
                }
            } else {
                if (isOverdue) modulOverdue = true
                else modulHasPending = true
            }
        }

        val modulStatus = when {
            modulTasks.isEmpty() -> ModulAjarProgressStatus.NO_TASK
            modulCompleted == modulTasks.size && modulOnTime == modulTasks.size -> ModulAjarProgressStatus.COMPLETED_ON_TIME
            modulCompleted == modulTasks.size -> ModulAjarProgressStatus.COMPLETED_LATE
            modulOverdue -> ModulAjarProgressStatus.OVERDUE
            modulHasPending -> ModulAjarProgressStatus.PENDING
            else -> ModulAjarProgressStatus.PENDING
        }

        return TeacherSupervisionSummary(
            teacher = teacher,
            totalAssignedTasks = applicableTasks.size,
            completedTasks = totalCompleted,
            onTimeCompletedTasks = onTimeCount,
            lateTasks = lateCount,
            pendingTasks = pendingCount,
            onTimeRatePercent = onTimeRate.coerceIn(0, 100),
            modulAjarTotal = modulTasks.size,
            modulAjarCompleted = modulCompleted,
            modulAjarOnTime = modulOnTime,
            modulAjarStatus = modulStatus,
            recentSubmissions = teacherSubmissions
        )
    }

    fun calculateSchoolOverview(
        teachers: List<User>,
        summaries: List<TeacherSupervisionSummary>
    ): SchoolSupervisionOverview {
        if (teachers.isEmpty() || summaries.isEmpty()) {
            return SchoolSupervisionOverview(
                totalTeachers = teachers.size,
                activeTeachers = teachers.count { it.isApproved },
                averageOnTimeRatePercent = 100,
                totalOnTimeTasks = 0,
                totalLateOrPendingTasks = 0,
                totalModulAjarAssigned = 0,
                totalModulAjarCompletedOnTime = 0,
                modulAjarCompliancePercent = 100
            )
        }

        val avgOnTimeRate = summaries.map { it.onTimeRatePercent }.average().toInt()
        val totalOnTime = summaries.sumOf { it.onTimeCompletedTasks }
        val totalLateOrPending = summaries.sumOf { it.lateTasks + it.pendingTasks }
        val totalModulAssigned = summaries.sumOf { it.modulAjarTotal }
        val totalModulOnTime = summaries.sumOf { it.modulAjarOnTime }

        val modulCompliance = if (totalModulAssigned > 0) {
            ((totalModulOnTime.toDouble() / totalModulAssigned.toDouble()) * 100).toInt()
        } else {
            100
        }

        return SchoolSupervisionOverview(
            totalTeachers = teachers.size,
            activeTeachers = teachers.count { it.isApproved },
            averageOnTimeRatePercent = avgOnTimeRate.coerceIn(0, 100),
            totalOnTimeTasks = totalOnTime,
            totalLateOrPendingTasks = totalLateOrPending,
            totalModulAjarAssigned = totalModulAssigned,
            totalModulAjarCompletedOnTime = totalModulOnTime,
            modulAjarCompliancePercent = modulCompliance.coerceIn(0, 100)
        )
    }
}
