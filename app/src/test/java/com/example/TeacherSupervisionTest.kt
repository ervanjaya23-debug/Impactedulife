package com.example

import com.example.model.ModulAjarProgressStatus
import com.example.model.SubmissionStatus
import com.example.model.Task
import com.example.model.TaskScope
import com.example.model.TaskSubmission
import com.example.model.TeacherSupervisionCalculator
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TeacherSupervisionTest {

    @Test
    fun testTeacherSummary_onTimeCompletionAndModulAjar() {
        val teacher1 = User(
            id = "t1",
            displayName = "Pak Budi",
            role = UserRole.TEACHER.name,
            subjectSpecialty = "Matematika"
        )

        val now = System.currentTimeMillis()
        val futureDue = Timestamp(Date(now + 86400000)) // +1 day
        val pastDue = Timestamp(Date(now - 86400000))   // -1 day

        val taskModulAjar = Task(
            id = "modul_1",
            title = "Pengumpulan Modul Ajar Semester 1",
            subjectOrCategory = "Modul Ajar & RPP",
            scope = TaskScope.TEACHER_TASK.name,
            targetFilter = "SEMUA GURU",
            dueDate = futureDue
        )

        val taskRaport = Task(
            id = "raport_1",
            title = "Upload Nilai Raport PTS",
            subjectOrCategory = "Penginputan Nilai Raport & PTS",
            scope = TaskScope.TEACHER_TASK.name,
            targetFilter = "SEMUA GURU",
            dueDate = futureDue
        )

        val taskAkreditasiOverdue = Task(
            id = "akreditasi_1",
            title = "Dokumen Akreditasi",
            subjectOrCategory = "Administrasi Akreditasi",
            scope = TaskScope.TEACHER_TASK.name,
            targetFilter = "SEMUA GURU",
            dueDate = pastDue
        )

        val tasks = listOf(taskModulAjar, taskRaport, taskAkreditasiOverdue)

        // Submissions:
        // Teacher submitted Modul Ajar on-time
        // Teacher submitted Raport on-time
        // Teacher has NOT submitted Akreditasi (which is past due -> late)
        val submissions = listOf(
            TaskSubmission(
                taskId = "modul_1",
                userId = "t1",
                userRole = UserRole.TEACHER.name,
                status = SubmissionStatus.COMPLETED.name,
                isOntime = true
            ),
            TaskSubmission(
                taskId = "raport_1",
                userId = "t1",
                userRole = UserRole.TEACHER.name,
                status = SubmissionStatus.COMPLETED.name,
                isOntime = true
            )
        )

        val summary = TeacherSupervisionCalculator.calculateTeacherSummary(
            teacher = teacher1,
            teacherTasks = tasks,
            submissions = submissions,
            currentTimeMillis = now
        )

        assertEquals(3, summary.totalAssignedTasks)
        assertEquals(2, summary.completedTasks)
        assertEquals(2, summary.onTimeCompletedTasks)
        assertEquals(1, summary.lateTasks) // 1 overdue unsubmitted task
        assertEquals(0, summary.pendingTasks)
        assertEquals(66, summary.onTimeRatePercent) // 2 / 3 = 66%

        // Modul Ajar specific status
        assertEquals(1, summary.modulAjarTotal)
        assertEquals(1, summary.modulAjarCompleted)
        assertEquals(1, summary.modulAjarOnTime)
        assertEquals(ModulAjarProgressStatus.COMPLETED_ON_TIME, summary.modulAjarStatus)
    }

    @Test
    fun testTeacherSummary_modulAjarOverdue() {
        val teacher2 = User(
            id = "t2",
            displayName = "Ibu Siti",
            role = UserRole.TEACHER.name,
            subjectSpecialty = "Bahasa Indonesia"
        )

        val now = System.currentTimeMillis()
        val pastDue = Timestamp(Date(now - 86400000))

        val taskModulAjarPastDue = Task(
            id = "modul_overdue",
            title = "Modul Ajar Kurikulum Merdeka",
            subjectOrCategory = "Modul Ajar & RPP",
            scope = TaskScope.TEACHER_TASK.name,
            targetFilter = "SEMUA GURU",
            dueDate = pastDue
        )

        val summary = TeacherSupervisionCalculator.calculateTeacherSummary(
            teacher = teacher2,
            teacherTasks = listOf(taskModulAjarPastDue),
            submissions = emptyList(),
            currentTimeMillis = now
        )

        assertEquals(1, summary.totalAssignedTasks)
        assertEquals(0, summary.completedTasks)
        assertEquals(0, summary.onTimeCompletedTasks)
        assertEquals(1, summary.lateTasks)
        assertEquals(ModulAjarProgressStatus.OVERDUE, summary.modulAjarStatus)
    }

    @Test
    fun testSchoolOverviewCalculation() {
        val teacher1 = User(id = "t1", role = UserRole.TEACHER.name)
        val teacher2 = User(id = "t2", role = UserRole.TEACHER.name)

        val summary1 = TeacherSupervisionCalculator.calculateTeacherSummary(
            teacher = teacher1,
            teacherTasks = emptyList(),
            submissions = emptyList()
        )
        val summary2 = TeacherSupervisionCalculator.calculateTeacherSummary(
            teacher = teacher2,
            teacherTasks = emptyList(),
            submissions = emptyList()
        )

        val overview = TeacherSupervisionCalculator.calculateSchoolOverview(
            teachers = listOf(teacher1, teacher2),
            summaries = listOf(summary1, summary2)
        )

        assertEquals(2, overview.totalTeachers)
        assertEquals(100, overview.averageOnTimeRatePercent)
        assertEquals(100, overview.modulAjarCompliancePercent)
    }
}
