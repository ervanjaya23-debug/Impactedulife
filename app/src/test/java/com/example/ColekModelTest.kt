package com.example

import com.example.model.ColekTemplates
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskScope
import com.example.model.User
import com.example.model.UserRole
import com.example.model.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColekModelTest {

    @Test
    fun userModel_roleAndApprovalChecks() {
        val student = User(
            id = "s1",
            role = UserRole.STUDENT.name,
            status = UserStatus.PENDING.name
        )
        assertTrue(student.isStudent)
        assertFalse(student.isTeacher)
        assertFalse(student.isPrincipal)
        assertFalse(student.isApproved)

        val principal = User(
            id = "p1",
            role = UserRole.PRINCIPAL.name,
            status = UserStatus.ACTIVE.name
        )
        assertTrue(principal.isPrincipal)
        assertTrue(principal.isApproved)
    }

    @Test
    fun colekTemplates_formatWithTaskTitle() {
        val title = "Modul Ajar"
        val formatted = String.format(ColekTemplates.principalToTeacher[0], title)
        assertTrue(formatted.contains("Modul Ajar"))
        assertTrue(formatted.contains("Bapak/Ibu Guru yang terhormat"))
    }

    @Test
    fun colekTemplates_streakMotivation() {
        val streak0 = ColekTemplates.getStreakMotivation(0L)
        val streak5 = ColekTemplates.getStreakMotivation(5L)
        assertTrue(streak0.contains("Mulai streak"))
        assertTrue(streak5.contains("5 hari"))
    }

    @Test
    fun taskModel_scopeAndDefaultOffsets() {
        val task = Task(
            id = "t1",
            schoolId = "sch1",
            scope = TaskScope.TEACHER_TASK.name,
            title = "Upload Nilai",
            priority = TaskPriority.HIGH.name
        )
        assertEquals(4, task.reminderOffsets.size)
        assertTrue(task.reminderOffsets.contains(1440L))
        assertTrue(task.reminderOffsets.contains(30L))
    }

    @Test
    fun disciplineStreak_tiersAndProgress() {
        // Pemula Disiplin (0 - 2)
        val studentBeginner = User(id = "s1", onTimeStreakCount = 1L)
        val tier1 = studentBeginner.getDisciplineTier()
        assertEquals("Pemula Disiplin", tier1.name)
        assertEquals(3L, tier1.nextTarget)
        assertEquals(2L, tier1.tasksRemaining(1L))
        assertEquals(1f / 3f, tier1.calculateProgress(1L), 0.01f)

        // Pejuang Waktu (3 - 6)
        val studentFighter = User(id = "s2", onTimeStreakCount = 5L)
        val tier2 = studentFighter.getDisciplineTier()
        assertEquals("Pejuang Waktu", tier2.name)
        assertEquals(7L, tier2.nextTarget)
        assertEquals(2L, tier2.tasksRemaining(5L))
        // Progress within 3..7 is (5-3)/(7-3) = 2/4 = 0.5f
        assertEquals(0.5f, tier2.calculateProgress(5L), 0.01f)

        // Bintang Teladan (7 - 14)
        val studentStar = User(id = "s3", onTimeStreakCount = 7L)
        assertEquals("Bintang Teladan", studentStar.getDisciplineTier().name)

        // Master Kedisiplinan (15 - 29)
        val studentMaster = User(id = "s4", onTimeStreakCount = 15L)
        assertEquals("Master Kedisiplinan", studentMaster.getDisciplineTier().name)

        // Legenda Kedisiplinan (30+)
        val studentLegend = User(id = "s5", onTimeStreakCount = 35L)
        assertEquals("Legenda Kedisiplinan", studentLegend.getDisciplineTier().name)
    }

    @Test
    fun disciplineStreak_onTimeRatePercent() {
        val user = User(
            id = "s1",
            totalTasksCompleted = 10L,
            totalOnTimeCompleted = 9L
        )
        assertEquals(90, user.onTimeRatePercent)
    }

    @Test
    fun disciplineStreak_celebrationMessages() {
        val msg1 = ColekTemplates.getOnTimeSuccessMessage(1L, false, "")
        assertTrue(msg1.contains("1 tugas selesai tepat waktu"))

        val msgTier = ColekTemplates.getOnTimeSuccessMessage(7L, true, "Bintang Teladan")
        assertTrue(msgTier.contains("Bintang Teladan"))
        assertTrue(msgTier.contains("SELAMAT"))
    }

    @Test
    fun colekSantun_calculateRemainingTime() {
        val now = 1000000000L
        assertEquals("Tanpa tenggat", ColekTemplates.calculateRemainingTime(0L, now))
        assertEquals("Tenggat telah lewat", ColekTemplates.calculateRemainingTime(now - 1000L, now))

        // 45 minutes
        val in45Min = now + (45L * 60L * 1000L)
        assertEquals("45 menit lagi", ColekTemplates.calculateRemainingTime(in45Min, now))

        // 14 hours
        val in14Hours = now + (14L * 3600L * 1000L)
        assertEquals("14 jam lagi", ColekTemplates.calculateRemainingTime(in14Hours, now))

        // 1 day 6 hours
        val in30Hours = now + (30L * 3600L * 1000L)
        assertEquals("1 hari 6 jam lagi", ColekTemplates.calculateRemainingTime(in30Hours, now))

        // 3 days
        val in3Days = now + (3L * 24L * 3600L * 1000L)
        assertEquals("3 hari lagi", ColekTemplates.calculateRemainingTime(in3Days, now))
    }

    @Test
    fun colekSantun_isApproachingDeadline() {
        val now = 1000000000L
        // within 48h
        val in12h = now + (12L * 3600L * 1000L)
        assertTrue(ColekTemplates.isApproachingDeadline(in12h, now, 48))

        // within 24h
        val in20h = now + (20L * 3600L * 1000L)
        assertTrue(ColekTemplates.isApproachingDeadline(in20h, now, 24))

        // over 48h
        val in72h = now + (72L * 3600L * 1000L)
        assertFalse(ColekTemplates.isApproachingDeadline(in72h, now, 48))

        // past due
        assertFalse(ColekTemplates.isApproachingDeadline(now - 1000L, now, 48))
        // zero due
        assertFalse(ColekTemplates.isApproachingDeadline(0L, now, 48))
    }

    @Test
    fun colekSantun_deadlineUrgencyLevels() {
        val now = 1000000000L
        assertEquals(com.example.model.DeadlineUrgency.OVERDUE, ColekTemplates.getDeadlineUrgency(now - 5000L, now))
        assertEquals(com.example.model.DeadlineUrgency.CRITICAL, ColekTemplates.getDeadlineUrgency(now + (3L * 3600L * 1000L), now))
        assertEquals(com.example.model.DeadlineUrgency.HIGH, ColekTemplates.getDeadlineUrgency(now + (15L * 3600L * 1000L), now))
        assertEquals(com.example.model.DeadlineUrgency.MODERATE, ColekTemplates.getDeadlineUrgency(now + (36L * 3600L * 1000L), now))
        assertEquals(com.example.model.DeadlineUrgency.NORMAL, ColekTemplates.getDeadlineUrgency(now + (72L * 3600L * 1000L), now))
        assertEquals(com.example.model.DeadlineUrgency.NORMAL, ColekTemplates.getDeadlineUrgency(0L, now))
    }

    @Test
    fun colekSantun_buildAutoPoliteMessage_principalToTeacher() {
        val now = 1000000000L
        val due = now + (14L * 3600L * 1000L)
        val msg = ColekTemplates.buildAutoPoliteMessage(
            senderRole = "PRINCIPAL",
            targetRole = "TEACHER",
            targetName = "Siti Rahmawati, M.Pd.",
            taskTitle = "Modul Ajar Bab 4",
            dueMillis = due,
            now = now
        )

        assertTrue(msg.contains("Yth. Bapak/Ibu Siti Rahmawati, M.Pd."))
        assertTrue(msg.contains("salam hormat"))
        assertTrue(msg.contains("Modul Ajar Bab 4"))
        assertTrue(msg.contains("14 jam lagi"))
        assertTrue(msg.contains("Mohon kesediaannya"))
        assertTrue(msg.contains("Terima kasih atas dedikasi"))
    }

    @Test
    fun colekSantun_buildAutoPoliteMessage_teacherToStudent() {
        val now = 1000000000L
        val due = now + (5L * 3600L * 1000L)
        val msg = ColekTemplates.buildAutoPoliteMessage(
            senderRole = "TEACHER",
            targetRole = "STUDENT",
            targetName = "Ahmad",
            taskTitle = "PR Matematika Trigonometri",
            dueMillis = due,
            now = now
        )

        assertTrue(msg.contains("ananda Ahmad"))
        assertTrue(msg.contains("PR Matematika Trigonometri"))
        assertTrue(msg.contains("5 jam lagi"))
        assertTrue(msg.contains("teliti dan penuh tanggung jawab"))
        assertTrue(msg.contains("Tetap semangat!"))
    }

    @Test
    fun colekSantun_buildAutoPoliteMessage_studentToStudent() {
        val now = 1000000000L
        val due = now + (20L * 3600L * 1000L)
        val msg = ColekTemplates.buildAutoPoliteMessage(
            senderRole = "STUDENT",
            targetRole = "STUDENT",
            targetName = "Budi",
            taskTitle = "Laporan Fisika Gerak Melingkar",
            dueMillis = due,
            now = now
        )

        assertTrue(msg.contains("Halo rekan Budi"))
        assertTrue(msg.contains("salam santun"))
        assertTrue(msg.contains("Laporan Fisika Gerak Melingkar"))
        assertTrue(msg.contains("streak kedisiplinan"))
    }

    @Test
    fun colekSantun_getTemplatesForTone() {
        val now = 1000000000L
        val due = now + (18L * 3600L * 1000L)

        val formalList = ColekTemplates.getTemplatesForTone(
            com.example.model.ColekSantunTone.FORMAL_RESPECTFUL,
            "Dr. Hendra",
            "Laporan BOS",
            due,
            now
        )
        assertTrue(formalList.isNotEmpty())
        assertTrue(formalList[0].contains("Dr. Hendra"))
        assertTrue(formalList[0].contains("Laporan BOS"))
        assertTrue(formalList[0].contains("18 jam lagi"))

        val supportiveList = ColekTemplates.getTemplatesForTone(
            com.example.model.ColekSantunTone.POLITE_SUPPORTIVE,
            "Rian",
            "Tugas Seni Budaya",
            due,
            now
        )
        assertTrue(supportiveList.isNotEmpty())
        assertTrue(supportiveList[0].contains("Salam semangat"))

        val peerList = ColekTemplates.getTemplatesForTone(
            com.example.model.ColekSantunTone.PEER_COURTEOUS,
            "Rini",
            "Tugas Sejarah",
            due,
            now
        )
        assertTrue(peerList.isNotEmpty())
        assertTrue(peerList[0].contains("Halo kawan Rini"))

        val directList = ColekTemplates.getTemplatesForTone(
            com.example.model.ColekSantunTone.QUICK_PROFESSIONAL,
            "Semua",
            "Input Nilai",
            due,
            now
        )
        assertTrue(directList.isNotEmpty())
        assertTrue(directList[0].contains("Pengingat Santun"))
    }
}

