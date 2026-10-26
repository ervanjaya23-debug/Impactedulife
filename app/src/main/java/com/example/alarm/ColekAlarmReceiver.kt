package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ColekAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "colek_alarm_channel_high"
        const val CHANNEL_NAME = "Alarm & Colek Santun Tugas"
        const val ACTION_TRIGGER_ALARM = "com.aistudio.colek.ACTION_TRIGGER_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.aistudio.colek.ACTION_SNOOZE_ALARM"
        const val ACTION_MARK_DONE = "com.aistudio.colek.ACTION_MARK_DONE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_SUBJECT = "extra_task_subject"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
        const val EXTRA_TARGET_SCHOOL = "extra_target_school"
        const val EXTRA_TARGET_FILTER = "extra_target_filter"
        const val EXTRA_COLEK_MESSAGE = "extra_colek_message"
        const val EXTRA_IS_TEACHER_TASK = "extra_is_teacher_task"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Tugas Sekolah"
        val taskSubject = intent.getStringExtra(EXTRA_TASK_SUBJECT) ?: "Administrasi"
        val offsetMinutes = intent.getLongExtra(EXTRA_OFFSET_MINUTES, 0L)
        val targetSchool = intent.getStringExtra(EXTRA_TARGET_SCHOOL).orEmpty()
        val targetFilter = intent.getStringExtra(EXTRA_TARGET_FILTER).orEmpty()
        val colekMessage = intent.getStringExtra(EXTRA_COLEK_MESSAGE).orEmpty()
        val isTeacherTask = intent.getBooleanExtra(EXTRA_IS_TEACHER_TASK, false)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_TRIGGER_ALARM -> {
                showAlarmNotification(
                    context = context,
                    notificationManager = notificationManager,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    taskSubject = taskSubject,
                    offsetMinutes = offsetMinutes,
                    targetSchool = targetSchool,
                    targetFilter = targetFilter,
                    colekMessage = colekMessage,
                    isTeacherTask = isTeacherTask
                )
            }
            ACTION_SNOOZE_ALARM -> {
                val notifId = taskId.hashCode()
                notificationManager.cancel(notifId)
                ColekAlarmScheduler(context).scheduleSnooze(taskId, taskTitle, 10L, isTeacherTask)
            }
            ACTION_MARK_DONE -> {
                val notifId = taskId.hashCode()
                notificationManager.cancel(notifId)
            }
        }
    }

    fun showAlarmNotification(
        context: Context,
        notificationManager: NotificationManager,
        taskId: String,
        taskTitle: String,
        taskSubject: String,
        offsetMinutes: Long,
        targetSchool: String = "",
        targetFilter: String = "",
        colekMessage: String = "",
        isTeacherTask: Boolean = false
    ) {
        createNotificationChannel(notificationManager)

        val notifId = taskId.hashCode()

        // Content Intent (open app)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Intent
        val snoozeIntent = Intent(context, ColekAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_IS_TEACHER_TASK, isTeacherTask)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done Intent
        val doneIntent = Intent(context, ColekAlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val schoolTag = if (targetSchool.isNotBlank()) " • 🏫 $targetSchool" else ""
        val targetTag = if (targetFilter.isNotBlank()) " • 🎯 $targetFilter" else ""

        val timeBadge = when {
            offsetMinutes >= 2880L -> "H-2 Hari menuju batas waktu"
            offsetMinutes >= 1440L -> "H-1 Hari menuju batas waktu"
            offsetMinutes >= 180L -> "Mendesak: H-3 Jam lagi!"
            offsetMinutes >= 30L -> "Segera: H-30 Menit lagi!"
            offsetMinutes > 0L -> "Kurang dari $offsetMinutes menit lagi!"
            else -> "⏰ Batas waktu tugas hampir tiba!"
        }

        val notifTitle = if (isTeacherTask) {
            "🔔 Colek Santun Guru: $taskTitle"
        } else {
            "⏰ Colek Alarm Tugas: $taskTitle"
        }

        val subtitle = "$timeBadge ($taskSubject$schoolTag$targetTag)"

        val politeDetailMessage = if (colekMessage.isNotBlank()) {
            colekMessage
        } else if (isTeacherTask) {
            "Yth. Bapak/Ibu Guru, mohon izin mengingatkan dengan santun bahwa tugas '$taskTitle' ($taskSubject) mendekati tenggat waktu. Terima kasih atas dedikasi Bapak/Ibu! 🙏✨"
        } else {
            "Yuk selesaikan tugas '$taskTitle' sebelum batas waktu berakhir dan pertahankan streak kedisiplinanmu! 🔥📚"
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val bannerBitmap = try {
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_colek_alarm_banner_1792976051626
            )
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(subtitle)
            .setSubText(if (targetSchool.isNotBlank()) targetSchool else "Pengingat Colek Santun")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Tunda 10 Mnt", snoozePendingIntent)
            .addAction(0, "Buka Tugas", contentPendingIntent)
            .addAction(0, "Selesai ✓", donePendingIntent)

        if (bannerBitmap != null) {
            builder.setLargeIcon(bannerBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setBigContentTitle(notifTitle)
                    .setSummaryText("$subtitle — $politeDetailMessage")
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$subtitle\n\n$politeDetailMessage")
            )
        }

        notificationManager.notify(notifId, builder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi alarm dan Colek Santun bergambar untuk pengingat batas waktu tugas Guru dan Siswa"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
