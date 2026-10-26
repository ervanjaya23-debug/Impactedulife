package com.example.model

import java.util.concurrent.TimeUnit

enum class ColekSantunTone(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    FORMAL_RESPECTFUL(
        id = "FORMAL",
        title = "Formal & Hormat",
        description = "Bahasa resmi beretika tinggi untuk atasan, dewan guru, atau rekan kerja",
        iconEmoji = "🎩"
    ),
    POLITE_SUPPORTIVE(
        id = "SUPPORTIVE",
        title = "Santun & Suportif",
        description = "Bahasa halus yang membangkitkan semangat dan perhatian bersahabat",
        iconEmoji = "🤝"
    ),
    PEER_COURTEOUS(
        id = "PEER",
        title = "Ramah Santun Teman",
        description = "Bahasa akrab antar teman belajar yang tetap beradab dan sopan",
        iconEmoji = "✨"
    ),
    QUICK_PROFESSIONAL(
        id = "DIRECT",
        title = "Lugas & Profesional",
        description = "Pengingat singkat, jelas, dan fokus pada batas waktu",
        iconEmoji = "⏱️"
    )
}

enum class DeadlineUrgency(val label: String, val badgeColorHex: String) {
    CRITICAL("Segera Jatuh Tempo (<6 Jam)", "#E53935"),
    HIGH("Mendekati Deadline (<24 Jam)", "#FB8C00"),
    MODERATE("Perhatian Tenggat (<48 Jam)", "#FDD835"),
    NORMAL("Waktu Masih Cukup", "#43A047"),
    OVERDUE("Lewat Tenggat Waktu", "#D32F2F")
}

object ColekTemplates {

    val studentToStudent = listOf(
        "Psst! Tugas %s tinggal sebentar lagi nih, yuk kita selesaikan bareng 💪",
        "Jangan biarkan tugas %s menangis di pojokan sendirian 🥺",
        "Ayo kerjakan sekarang biar nanti malam tenang bisa santai atau mabar 🎮",
        "Colek tipis-tipis! Tugas %s jangan sampai kelewatan deadline ya kawan ✨",
        "Biar streak belajar kita tetap menyala, gas selesaikan %s yuk! 🔥"
    )

    val principalToTeacher = listOf(
        "Bapak/Ibu Guru yang terhormat, pengingat ramah untuk pengumpulan %s sebelum batas waktu demi kelancaran kurikulum sekolah 🙏",
        "Salam semangat Bapak/Ibu Guru! Mohon kesediaannya melengkapi %s. Terima kasih banyak atas dedikasinya ✨",
        "Mengingatkan kembali penginputan berkas %s. Semoga prosesnya dimudahkan dan berjalan lancar 📋",
        "Bapak/Ibu, mari kita selesaikan administrasi %s tepat waktu agar rekapitulasi sekolah optimal 🏫"
    )

    val teacherToStudent = listOf(
        "Halo anak-anak hebat, jangan lupa selesaikan tugas %s sebelum tenggat waktu ya 📚",
        "Pengingat kelas: Tugas %s sudah mendekati deadline. Kerjakan dengan teliti dan penuh semangat! ✨",
        "Ayo kumpulkan tugas %s tepat waktu agar nilaimu maksimal 🌟"
    )

    // -------------------------------------------------------------
    // TEMPLATE COLEK SANTUN (Sopan, Beretika & Profesional)
    // -------------------------------------------------------------

    val formalTemplates = listOf(
        "Yth. Bapak/Ibu %s, salam hormat. Izin mengingatkan dengan santun perihal pemenuhan tugas '%s' yang memiliki batas waktu pengumpulan %s. Mohon kesediaannya untuk menyelesaikan sebelum tenggat waktu demi kelancaran administrasi sekolah. Terima kasih atas dedikasi dan kerja samanya. 🙏",
        "Dengan hormat Bapak/Ibu %s. Sekadar pengingat santun untuk pengumpulan '%s' yang tersisa %s lagi. Kiranya ada kendala teknis atau kebutuhan koordinasi, kami siap membantu. Salam sukses dan sehat selalu. ✨",
        "Salam hormat Bapak/Ibu %s. Mengingatkan kembali perihal penyelesaian berkas '%s' (sisa waktu: %s) demi ketertiban rekapitulasi sekolah. Terima kasih banyak atas waktu dan perhatiannya yang berharga. 📋",
        "Kepada Yth. Rekan %s, dengan salam takzim. Mohon kesediaannya untuk menuntaskan tugas '%s' sebelum batas waktu (%s) agar target program kita dapat tercapai tepat waktu. Terima kasih. 🏛️"
    )

    val supportiveTemplates = listOf(
        "Salam semangat Bapak/Ibu %s! Izin mengingatkan dengan santun bahwa tugas '%s' tersisa %s lagi. Semoga proses pengerjaannya dilancarkan dan selalu diberikan kemudahan. Tetap sehat dan semangat! 🌟",
        "Halo rekan %s yang baik, semoga harimu menyenangkan. Sekadar pengingat ramah untuk tugas '%s' yang mendekati deadline (%s). Mari kita tuntaskan bersama, tetap semangat ya! 🤝",
        "Salam hangat ananda/rekan %s. Mengingatkan dengan santun perihal tugas '%s' (sisa %s). Kami percaya kamu bisa menyelesaikannya dengan hasil terbaik. Semangat berproses! 🌈",
        "Hai %s, terima kasih atas dedikasi dan kerja kerasmu. Izin memberikan pengingat santun bahwa tugas '%s' mendekati tenggat (%s). Jangan ragu berdiskusi jika ada kendala ya. ✨"
    )

    val peerTemplates = listOf(
        "Halo kawan %s, salam semangat! Mengingatkan dengan santun ya, tugas '%s' tersisa %s lagi nih. Yuk kita cicil dan selesaikan bersama agar streak kedisiplinan kita tetap menyala! 🔥",
        "Hai %s, izin colek santun perihal tugas '%s' (tenggat %s). Jangan lupa dikumpulkan tepat waktu ya kawan biar nanti malam bisa istirahat tenang! 📚✨",
        "Psst %s! Pengingat ramah untuk kita bersama: tugas '%s' tinggal %s lagi. Semoga lancar pengerjaannya dan nilainya memuaskan! 💪",
        "Halo rekan belajar %s, colek santun tipis-tipis! Tugas '%s' mendekati batas waktu (%s). Ayo kita tuntaskan sekarang! 🎯"
    )

    val directTemplates = listOf(
        "Pengingat Santun: Batas waktu pengumpulan tugas '%s' tersisa %s. Diharapkan penyelesaian sebelum batas waktu demi kelancaran rekapitulasi data. Terima kasih atas kerja samanya. ⏱️",
        "Pemberitahuan Profesional: Tugas '%s' memiliki tenggat waktu tersisa %s. Mohon konfirmasi penyelesaian tepat waktu. Terima kasih atas profesionalitasnya. 📌",
        "Pengingat Tenggat: Perhatian untuk pengumpulan tugas '%s' (sisa waktu: %s). Terima kasih banyak atas ketepatan waktunya. ⏳"
    )

    // -------------------------------------------------------------
    // HELPER FUNCTIONS & CALCULATORS
    // -------------------------------------------------------------

    fun calculateRemainingTime(dueMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (dueMillis <= 0L) return "Tanpa tenggat"
        val diff = dueMillis - now
        if (diff < 0) return "Tenggat telah lewat"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 60 -> "$minutes menit lagi"
            hours < 24 -> "$hours jam lagi"
            days < 2 -> "1 hari ${hours % 24} jam lagi"
            else -> "$days hari lagi"
        }
    }

    fun isApproachingDeadline(dueMillis: Long, now: Long = System.currentTimeMillis(), thresholdHours: Long = 48): Boolean {
        if (dueMillis <= 0L) return false
        val diff = dueMillis - now
        return diff in 1..TimeUnit.HOURS.toMillis(thresholdHours)
    }

    fun getDeadlineUrgency(dueMillis: Long, now: Long = System.currentTimeMillis()): DeadlineUrgency {
        if (dueMillis <= 0L) return DeadlineUrgency.NORMAL
        val diff = dueMillis - now
        return when {
            diff < 0 -> DeadlineUrgency.OVERDUE
            diff <= TimeUnit.HOURS.toMillis(6) -> DeadlineUrgency.CRITICAL
            diff <= TimeUnit.HOURS.toMillis(24) -> DeadlineUrgency.HIGH
            diff <= TimeUnit.HOURS.toMillis(48) -> DeadlineUrgency.MODERATE
            else -> DeadlineUrgency.NORMAL
        }
    }

    /**
     * Otomatis membangun kalimat pengingat santun dan profesional yang disesuaikan
     * dengan peran penerima, nama penerima, judul tugas, dan sisa waktu pengerjaan.
     */
    fun buildAutoPoliteMessage(
        senderRole: String,
        targetRole: String,
        targetName: String,
        taskTitle: String,
        dueMillis: Long,
        now: Long = System.currentTimeMillis()
    ): String {
        val remaining = calculateRemainingTime(dueMillis, now)
        val cleanName = targetName.ifBlank { "Rekan" }

        return when {
            senderRole == "PRINCIPAL" || targetRole == "TEACHER" -> {
                "Yth. Bapak/Ibu $cleanName, salam hormat. Izin mengingatkan dengan santun perihal tugas administrasi '$taskTitle' yang memiliki batas waktu $remaining. Mohon kesediaannya menyelesaikan sebelum tenggat waktu demi kelancaran kurikulum sekolah. Terima kasih atas dedikasi dan kerja samanya. 🙏"
            }
            senderRole == "TEACHER" -> {
                "Salam santun dan semangat ananda $cleanName. Pengingat ramah untuk tugas '$taskTitle' yang mendekati tenggat waktu ($remaining). Silakan diselesaikan dengan teliti dan penuh tanggung jawab ya. Tetap semangat! ✨"
            }
            else -> {
                "Halo rekan $cleanName, salam santun! Sekadar pengingat ramah bahwa tugas '$taskTitle' tersisa $remaining lagi. Yuk kita selesaikan tepat waktu agar streak kedisiplinan kita tetap menyala! 💪"
            }
        }
    }

    fun generateSmartReminder(
        recipientName: String,
        taskTitle: String,
        subjectOrCategory: String,
        dueMillis: Long,
        isForTeacher: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val cleanRecipient = recipientName.ifBlank { if (isForTeacher) "Bapak/Ibu Guru" else "Siswa" }
        val cleanCategory = subjectOrCategory.ifBlank { "Tugas Sekolah" }
        val remaining = calculateRemainingTime(dueMillis, nowMillis)
        return if (isForTeacher) {
            "Yth. Bapak/Ibu $cleanRecipient, salam hormat. Izin memberikan pengingat 'Colek Santun' perihal tugas $cleanCategory '$taskTitle' yang mendekati batas waktu ($remaining). Mohon kesediaannya menyelesaikan sebelum tenggat waktu demi kelancaran kurikulum sekolah. Terima kasih atas dedikasi Bapak/Ibu. 🙏"
        } else {
            "Salam semangat $cleanRecipient! Pengingat ramah untuk tugas $cleanCategory '$taskTitle' yang mendekati batas waktu ($remaining). Ayo tuntaskan tepat waktu! ✨"
        }
    }

    fun getTemplatesForTone(
        tone: ColekSantunTone,
        targetName: String,
        taskTitle: String,
        dueMillis: Long,
        now: Long = System.currentTimeMillis()
    ): List<String> {
        val remaining = calculateRemainingTime(dueMillis, now)
        val cleanName = targetName.ifBlank { "Rekan" }

        return when (tone) {
            ColekSantunTone.FORMAL_RESPECTFUL -> formalTemplates.map { tmpl ->
                try {
                    String.format(tmpl, cleanName, taskTitle, remaining)
                } catch (_: Exception) {
                    tmpl
                }
            }
            ColekSantunTone.POLITE_SUPPORTIVE -> supportiveTemplates.map { tmpl ->
                try {
                    String.format(tmpl, cleanName, taskTitle, remaining)
                } catch (_: Exception) {
                    tmpl
                }
            }
            ColekSantunTone.PEER_COURTEOUS -> peerTemplates.map { tmpl ->
                try {
                    String.format(tmpl, cleanName, taskTitle, remaining)
                } catch (_: Exception) {
                    tmpl
                }
            }
            ColekSantunTone.QUICK_PROFESSIONAL -> directTemplates.map { tmpl ->
                try {
                    String.format(tmpl, taskTitle, remaining)
                } catch (_: Exception) {
                    tmpl
                }
            }
        }
    }

    fun getStreakMotivation(streakDays: Long): String {
        return when {
            streakDays == 0L -> "Mulai streak pertamamu hari ini! Kumpulkan tugas tepat waktu 🔥"
            streakDays < 3L -> "Api semangatmu mulai menyala! Pertahankan $streakDays hari berturut-turut! 🔥"
            streakDays < 7L -> "Luar biasa! $streakDays hari disiplin tanpa terlambat! Duolingo pun bangga 🔥✨"
            else -> "Juara bertahan! $streakDays hari streak emas, kamu teladan kedisiplinan sekolah! 👑🔥"
        }
    }

    fun getOnTimeSuccessMessage(currentStreak: Long, isNewTier: Boolean, tierName: String): String {
        return when {
            isNewTier -> "🎉 SELAMAT! Kamu naik peringkat menjadi $tierName! Kedisiplinanmu luar biasa! 🏆"
            currentStreak == 1L -> "Hebat! 1 tugas selesai tepat waktu! Awal mula streak kedisiplinanmu 🔥"
            currentStreak < 3L -> "Mantap! $currentStreak tugas beruntun tepat waktu! Lanjutkan ke target 3 tugas 💪"
            currentStreak < 7L -> "Luar biasa! $currentStreak tugas berturut-turut on-time! Api kedisiplinanmu menyala terang! ⚡🔥"
            currentStreak < 15L -> "Menakjubkan! Rekor $currentStreak tugas tepat waktu! Kamu bintang teladan kelas! 🌟✨"
            else -> "Kedisiplinan tingkat dewa! $currentStreak tugas berturut-turut on-time tanpa cela! 👑💎"
        }
    }

    fun getInactivityNudge(inactiveDays: Int): String {
        return when (inactiveDays) {
            1 -> "Streak-mu hampir dingin! Buka aplikasi dan tuntaskan tugas hari ini agar apinya tetap membara 🔥"
            3 -> "Tugas sekolahmu kangen disentuh nih 🥺 Jangan biarkan menumpuk ya!"
            else -> "Sudah $inactiveDays hari tidak aktif! Mari mulai dari satu tugas kecil sekarang juga 🚀"
        }
    }
}
