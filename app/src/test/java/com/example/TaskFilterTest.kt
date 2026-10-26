package com.example

import com.example.model.SubmissionStatus
import com.example.model.Task
import com.example.model.TaskSubmission
import com.example.ui.screens.TaskStatusFilter
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TaskFilterTest {

    @Test
    fun testTaskStatusFilterEnumValues() {
        assertEquals("Semua", TaskStatusFilter.ALL.label)
        assertEquals("Belum Selesai", TaskStatusFilter.UNCOMPLETED.label)
        assertEquals("Selesai", TaskStatusFilter.COMPLETED.label)
        assertEquals("Terlambat", TaskStatusFilter.LATE.label)
    }

    @Test
    fun testFilteringLogic() {
        val now = System.currentTimeMillis()
        val pastDue = Timestamp(Date(now - 3600000))
        val futureDue = Timestamp(Date(now + 3600000))

        val taskOnTimePending = Task(id = "task_1", title = "Tugas Aktif", dueDate = futureDue)
        val taskOverduePending = Task(id = "task_2", title = "Tugas Terlambat Belum Selesai", dueDate = pastDue)
        val taskCompletedOnTime = Task(id = "task_3", title = "Tugas Selesai Tepat Waktu", dueDate = futureDue)
        val taskCompletedLate = Task(id = "task_4", title = "Tugas Selesai Terlambat", dueDate = pastDue)

        val submissions = listOf(
            TaskSubmission(taskId = "task_3", status = SubmissionStatus.COMPLETED.name, isOntime = true),
            TaskSubmission(taskId = "task_4", status = SubmissionStatus.LATE.name, isOntime = false)
        )

        val tasks = listOf(taskOnTimePending, taskOverduePending, taskCompletedOnTime, taskCompletedLate)

        // Status evaluation helper
        val statusMap = tasks.associateWith { task ->
            val sub = submissions.firstOrNull { it.taskId == task.id }
            val isCompleted = sub != null && (sub.status == SubmissionStatus.COMPLETED.name || sub.status == SubmissionStatus.LATE.name)
            val isSubmittedLate = sub?.status == SubmissionStatus.LATE.name || (sub != null && !sub.isOntime)
            val dueMillis = task.dueDate?.toDate()?.time ?: 0L
            val isOverdue = !isCompleted && dueMillis in 1 until now
            val isLate = isOverdue || isSubmittedLate
            Triple(isCompleted, isLate, isOverdue)
        }

        // Test uncompleted count: task_1, task_2
        val uncompleted = tasks.filter { statusMap[it]?.first == false }
        assertEquals(2, uncompleted.size)
        assertTrue(uncompleted.contains(taskOnTimePending))
        assertTrue(uncompleted.contains(taskOverduePending))

        // Test completed count: task_3, task_4
        val completed = tasks.filter { statusMap[it]?.first == true }
        assertEquals(2, completed.size)
        assertTrue(completed.contains(taskCompletedOnTime))
        assertTrue(completed.contains(taskCompletedLate))

        // Test late count: task_2 (overdue unfinished), task_4 (submitted late)
        val late = tasks.filter { statusMap[it]?.second == true }
        assertEquals(2, late.size)
        assertTrue(late.contains(taskOverduePending))
        assertTrue(late.contains(taskCompletedLate))
        assertFalse(late.contains(taskOnTimePending))
        assertFalse(late.contains(taskCompletedOnTime))
    }
}
