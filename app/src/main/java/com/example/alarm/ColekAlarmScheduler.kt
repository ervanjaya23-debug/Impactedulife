package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.ColekTemplates
import com.example.model.Task
import com.example.model.TaskScope
import java.util.Date

class ColekAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("colek_local_notif_prefs", Context.MODE_PRIVATE)

    companion object {
        // Offsets in minutes before deadline: H-2 days (2880m), H-1 day (1440m), H-6 hours (360m), H-3 hours (180m), H-30 mins (30m), On deadline (0m)
        val DEFAULT_TEACHER_OFFSETS = listOf(2880L, 1440L, 360L, 180L, 30L, 0L)
    }

    fun scheduleTaskAlarms(task: Task) {
        val dueMillis = task.dueDate?.toDate()?.time ?: return
        val now = System.currentTimeMillis()

        if (dueMillis <= now) {
            Log.d("ColekAlarmScheduler", "Task deadline already passed: ${task.title}")
            return
        }

        val isTeacherTask = task.scope == TaskScope.TEACHER_TASK.name
        val offsets = if (isTeacherTask) {
            (task.reminderOffsets + DEFAULT_TEACHER_OFFSETS).distinct()
        } else {
            task.reminderOffsets
        }

        val politeMessage = if (isTeacherTask) {
            ColekTemplates.generateSmartReminder(
                recipientName = task.targetFilter.ifBlank { "Bapak/Ibu Guru" },
                taskTitle = task.title,
                subjectOrCategory = task.subjectOrCategory.ifBlank { "Administrasi" },
                dueMillis = dueMillis,
                isForTeacher = true
            )
        } else {
            ""
        }

        offsets.forEachIndexed { index, offsetMinutes ->
            val triggerTime = dueMillis - (offsetMinutes * 60 * 1000L)
            if (triggerTime > now) {
                scheduleExactAlarm(
                    requestCode = (task.id.hashCode() + index * 100),
                    triggerAtMillis = triggerTime,
                    taskId = task.id,
                    taskTitle = task.title,
                    taskSubject = task.subjectOrCategory,
                    offsetMinutes = offsetMinutes,
                    targetSchool = task.targetSchoolName,
                    targetFilter = task.targetFilter,
                    colekMessage = politeMessage,
                    isTeacherTask = isTeacherTask
                )
            }
        }
    }

    /**
     * Automatically schedules exact alarms for all uncompleted teacher tasks and triggers an immediate
     * polite local 'Colek' notification if any teacher task is approaching its deadline (within 48h)
     * and hasn't been notified today.
     */
    fun syncAndNotifyApproachingTeacherTasks(
        teacherTasks: List<Task>,
        completedTaskIds: Set<String>,
        teacherName: String
    ): Int {
        val now = System.currentTimeMillis()
        val dayBucket = now / (12 * 60 * 60 * 1000L) // Once per 12-hour window per approaching task
        var notifiedCount = 0

        teacherTasks.forEach { task ->
            if (task.id !in completedTaskIds) {
                // Ensure exact alarms are scheduled
                scheduleTaskAlarms(task)

                val dueMillis = task.dueDate?.toDate()?.time ?: 0L
                val isApproaching = ColekTemplates.isApproachingDeadline(dueMillis, now)
                if (isApproaching) {
                    val prefKey = "notified_${task.id}_$dayBucket"
                    if (!prefs.getBoolean(prefKey, false)) {
                        prefs.edit().putBoolean(prefKey, true).apply()
                        triggerImmediateColekNotification(
                            task = task,
                            recipientName = teacherName.ifBlank { "Bapak/Ibu Guru" }
                        )
                        notifiedCount++
                    }
                }
            }
        }
        return notifiedCount
    }

    /**
     * Fires an immediate local notification with the attractive banner image (BigPictureStyle)
     * for a 'Colek Santun' reminder or alarm test.
     */
    fun triggerImmediateColekNotification(
        task: Task,
        recipientName: String = "Bapak/Ibu Guru",
        customMessage: String? = null
    ) {
        val now = System.currentTimeMillis()
        val dueMillis = task.dueDate?.toDate()?.time ?: (now + 24 * 3600 * 1000L)
        val remainingMinutes = ((dueMillis - now) / (60 * 1000L)).coerceAtLeast(0L)
        val isTeacherTask = task.scope == TaskScope.TEACHER_TASK.name

        val message = customMessage?.takeIf { it.isNotBlank() }
            ?: ColekTemplates.generateSmartReminder(
                recipientName = recipientName,
                taskTitle = task.title,
                subjectOrCategory = task.subjectOrCategory.ifBlank { "Administrasi" },
                dueMillis = dueMillis,
                isForTeacher = isTeacherTask,
                nowMillis = now
            )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ColekAlarmReceiver().showAlarmNotification(
            context = context,
            notificationManager = notificationManager,
            taskId = task.id.ifBlank { "preview_${System.currentTimeMillis()}" },
            taskTitle = task.title,
            taskSubject = task.subjectOrCategory.ifBlank { "Tugas Sekolah" },
            offsetMinutes = remainingMinutes,
            targetSchool = task.targetSchoolName,
            targetFilter = task.targetFilter,
            colekMessage = message,
            isTeacherTask = isTeacherTask
        )
    }

    fun scheduleSnooze(
        taskId: String,
        taskTitle: String,
        snoozeMinutes: Long = 10L,
        isTeacherTask: Boolean = false
    ) {
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        scheduleExactAlarm(
            requestCode = (taskId.hashCode() + 999),
            triggerAtMillis = triggerTime,
            taskId = taskId,
            taskTitle = taskTitle,
            taskSubject = "Tunda Pengingat ($snoozeMinutes Mnt)",
            offsetMinutes = 0L,
            isTeacherTask = isTeacherTask
        )
    }

    private fun scheduleExactAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        taskId: String,
        taskTitle: String,
        taskSubject: String,
        offsetMinutes: Long,
        targetSchool: String = "",
        targetFilter: String = "",
        colekMessage: String = "",
        isTeacherTask: Boolean = false
    ) {
        val intent = Intent(context, ColekAlarmReceiver::class.java).apply {
            action = ColekAlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(ColekAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ColekAlarmReceiver.EXTRA_TASK_TITLE, taskTitle)
            putExtra(ColekAlarmReceiver.EXTRA_TASK_SUBJECT, taskSubject)
            putExtra(ColekAlarmReceiver.EXTRA_OFFSET_MINUTES, offsetMinutes)
            putExtra(ColekAlarmReceiver.EXTRA_TARGET_SCHOOL, targetSchool)
            putExtra(ColekAlarmReceiver.EXTRA_TARGET_FILTER, targetFilter)
            putExtra(ColekAlarmReceiver.EXTRA_COLEK_MESSAGE, colekMessage)
            putExtra(ColekAlarmReceiver.EXTRA_IS_TEACHER_TASK, isTeacherTask)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("ColekAlarmScheduler", "Alarm scheduled for $taskTitle at ${Date(triggerAtMillis)}")
        } catch (e: Exception) {
            Log.e("ColekAlarmScheduler", "Failed to schedule exact alarm", e)
        }
    }

    fun cancelTaskAlarms(task: Task) {
        val offsets = (task.reminderOffsets + DEFAULT_TEACHER_OFFSETS).distinct()
        offsets.forEachIndexed { index, _ ->
            val requestCode = (task.id.hashCode() + index * 100)
            val intent = Intent(context, ColekAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
